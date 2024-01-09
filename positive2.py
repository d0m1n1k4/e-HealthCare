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
_HEART_RATE_MEASUREMENT_CHAR_UUID = bluetooth.UUID(0x2A37)
_BLOOD_GLUCOSE_UUID = bluetooth.UUID(0x1808)
_BLOOD_GLUCOSE_MEASUREMENT_CHAR_UUID = bluetooth.UUID(0x2A18)

_HEART_RATE_MEASUREMENT_CHAR = (
    _HEART_RATE_MEASUREMENT_CHAR_UUID,
    _FLAG_NOTIFY | _FLAG_INDICATE, 
)

_BLOOD_GLUCOSE_MEASUREMENT_CHAR = (
    _BLOOD_GLUCOSE_MEASUREMENT_CHAR_UUID,
    _FLAG_NOTIFY,
)

_HEART_RATE_SERVICE = (
    _HEART_RATE_UUID,
    (_HEART_RATE_MEASUREMENT_CHAR,),
)

_BLOOD_GLUCOSE_SERVICE = (
    _BLOOD_GLUCOSE_UUID,
    (_BLOOD_GLUCOSE_MEASUREMENT_CHAR,),
)

class BLEHealthSensor:
    def __init__(self, ble, name="Pico"):
        self._ble = ble
        self._ble.active(True)
        self._ble.irq(self._irq)
        ((self._hr_handle,), (self._bg_handle,)) = self._ble.gatts_register_services((_HEART_RATE_SERVICE, _BLOOD_GLUCOSE_SERVICE))
        self._connections = set()
        self._button_pressed = False
        self._state = 0

        self._button = Pin(0, Pin.IN, Pin.PULL_DOWN)
        self._button.irq(trigger=Pin.IRQ_FALLING, handler=self._handle_button_press)

        if len(name) == 0:
            name = 'Pico %s' % ubinascii.hexlify(self._ble.config('mac')[1], ':').decode().upper()
        self._payload = advertising_payload(
            name=name, services=[_HEART_RATE_UUID, _BLOOD_GLUCOSE_UUID]
        )
        self._advertise()

    def _irq(self, event, data):
        if event == _IRQ_CENTRAL_CONNECT:
            conn_handle, _, _ = data
            self._connections.add(conn_handle)
            print("Connected to central:", conn_handle)
        elif event == _IRQ_CENTRAL_DISCONNECT:
            conn_handle, _, _ = data
            self._connections.remove(conn_handle)
            self._advertise()

    def _handle_button_press(self, pin):
        self._button_pressed = True
        self.process_button_press()

    def process_button_press(self):
        if self._button_pressed:
            self._button_pressed = False

        if self._state == 0:
            self.update_heart_rate()
        elif self._state == 1:
            self.update_blood_glucose()
        elif self._state > 1:
            return

        self._state += 1


    def update_heart_rate(self):
        flags = 0b00000000 
        heart_rate_value = 96  
        heart_rate_measurement = struct.pack('<B', flags) + struct.pack('<B', heart_rate_value)
        print("write heart rate:", heart_rate_value)
        for conn_handle in self._connections:
            self._ble.gatts_notify(conn_handle, self._hr_handle, heart_rate_measurement)


    def update_blood_glucose(self):
        glucose_value = 120
        glucose_measurement = struct.pack("<H", glucose_value)
        print("write blood glucose:", glucose_value)
        for conn_handle in self._connections:
            self._ble.gatts_notify(conn_handle, self._bg_handle, glucose_measurement)

    def _advertise(self, interval_us=500000):
        self._ble.gap_advertise(interval_us, adv_data=self._payload)

def demo():
    ble = bluetooth.BLE()
    health_sensor = BLEHealthSensor(ble)

    while True:
        time.sleep_ms(200)

if __name__ == "__main__":
    demo()

