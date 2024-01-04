import bluetooth
import struct
import time
import machine
import ubinascii
from ble_advertising import advertising_payload
from micropython import const
from machine import Pin

_IRQ_CENTRAL_CONNECT = const(1)
_IRQ_CENTRAL_DISCONNECT = const(2)
_IRQ_GATTS_WRITE = const(3)
_IRQ_GATTS_INDICATE_DONE = const(20)

_FLAG_READ = const(0x0002)
_FLAG_NOTIFY = const(0x0010)
_FLAG_INDICATE = const(0x0020)

_HEART_RATE_UUID = bluetooth.UUID(0x180D)
_BLOOD_GLUCOSE_UUID = bluetooth.UUID(0x1808)

_HEART_RATE_CHAR = (
    bluetooth.UUID(0x2A37),
    _FLAG_NOTIFY | _FLAG_INDICATE,
)

_BLOOD_GLUCOSE_CHAR = (
    bluetooth.UUID(0x2A18),
    _FLAG_NOTIFY,
)

_HEART_RATE_SERVICE = (
    _HEART_RATE_UUID,
    (_HEART_RATE_CHAR,),
)

_BLOOD_GLUCOSE_SERVICE = (
    _BLOOD_GLUCOSE_UUID,
    (_BLOOD_GLUCOSE_CHAR,),
)

_CUSTOM_SERVICE_UUID = bluetooth.UUID(0x1234)
_CUSTOM_CHAR_UUID = (
    bluetooth.UUID(0x5678),
    _FLAG_READ | _FLAG_NOTIFY | _FLAG_INDICATE,
)

_CUSTOM_SERVICE = (
    _CUSTOM_SERVICE_UUID,
    (_CUSTOM_CHAR_UUID,),
)

class BLEHealthSensor:
    def __init__(self, ble, name=""):
        self._ble = ble
        self._ble.active(True)
        self._ble.irq(self._irq)
        ((self._hr_handle,), (self._bg_handle,)) = self._ble.gatts_register_services((_HEART_RATE_SERVICE, _BLOOD_GLUCOSE_SERVICE))
        ((self._custom_handle,),) = self._ble.gatts_register_services((_CUSTOM_SERVICE,))
        self._connections = set()
        self._button_pressed = False
        self._state = 0

        self._button = Pin(0, Pin.IN, Pin.PULL_DOWN)
        self._button.irq(trigger=Pin.IRQ_FALLING, handler=self._handle_button_press)

        if len(name) == 0:
            name = 'Pico %s' % ubinascii.hexlify(self._ble.config('mac')[1], ':').decode().upper()
            print('Sensor name %s' % name)
        self._payload = advertising_payload(
            name=name, services=[_HEART_RATE_UUID, _BLOOD_GLUCOSE_UUID]
        )
        self._advertise()

    def _irq(self, event, data):
        if event == _IRQ_CENTRAL_CONNECT:
            conn_handle, _, _ = data
            self._connections.add(conn_handle)
            print("Connected to central:", conn_handle)
            self.send_data_to_mobile(conn_handle, b"Hello from Pico!")
        elif event == _IRQ_CENTRAL_DISCONNECT:
            conn_handle, _, _ = data
            self._connections.remove(conn_handle)
            self._advertise()
        elif event == _IRQ_GATTS_INDICATE_DONE:
            conn_handle, value_handle, status = data
            if value_handle == self._hr_handle:
                self._heart_rate_measurement_complete = True
            elif value_handle == self._bg_handle:
                self._glucose_measurement_complete = True
        elif event == _IRQ_GATTS_WRITE:
            conn_handle, value_handle = data
            if value_handle == self._mobile_app_handle:
                received_data = self._ble.gatts_read(self._mobile_app_handle)
                print("Received data from mobile app:", received_data)

    def _handle_button_press(self, pin):
        self._button_pressed = True

    def _wait_for_button_release(self):
        while self._button.value() == 0:
            time.sleep_ms(10)

    def send_data_to_mobile(self, conn_handle, data):
        self._ble.gatts_write(self._custom_handle, data)
        self._ble.gatts_notify(conn_handle, self._custom_handle)

    def process_button_press(self):
        if self._button_pressed:
            self._button_pressed = False
            self._wait_for_button_release()

            if self._state == 0:
                self.update_heart_rate()
                self._state += 1
            elif self._state == 1:
                self.update_blood_glucose()
                self._state += 1
            elif self._state == 2:
                print("Sending data to central.")
                for conn_handle in self._connections:
                    self.send_data_to_mobile(conn_handle, b"Data updated")
                self._state += 1 

    def update_heart_rate(self):
        flags = 0b00000000  
        heart_rate_value = 150
        heart_rate_measurement = struct.pack("BB", flags, heart_rate_value)  
        print("write heart rate:", heart_rate_value)
        self._ble.gatts_write(self._hr_handle, heart_rate_measurement)

    def update_blood_glucose(self):
        glucose_value = 120
        glucose_measurement = struct.pack("<H", glucose_value)
        print("write blood glucose:", glucose_value)
        self._ble.gatts_write(self._bg_handle, glucose_measurement)

    def _advertise(self, interval_us=500000):
        self._ble.gap_advertise(interval_us, adv_data=self._payload)

def demo():
    ble = bluetooth.BLE()
    health_sensor = BLEHealthSensor(ble)

    while True:
        health_sensor.process_button_press()
        time.sleep_ms(200) 

if __name__ == "__main__":
    demo()
