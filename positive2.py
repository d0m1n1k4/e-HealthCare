import bluetooth
import random
import struct
import time
import machine
from ble_advertising import advertising_payload
from micropython import const
from machine import Pin

_IRQ_CENTRAL_CONNECT = const(1)
_IRQ_CENTRAL_DISCONNECT = const(2)
_IRQ_GATTS_INDICATE_DONE = const(20)

_FLAG_READ = const(0x0002)
_FLAG_NOTIFY = const(0x0010)
_FLAG_INDICATE = const(0x0020)

_HEART_RATE_UUID = bluetooth.UUID(0x180D)

_BLOOD_GLUCOSE_UUID = bluetooth.UUID(0x1808)

_HEART_RATE_CHAR = (
    bluetooth.UUID(0x2A37),
    _FLAG_NOTIFY | _FLAG_READ,
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

class BLEHealthSensor:
    def __init__(self, ble, name=""):
        self._ble = ble
        self._ble.active(True)
        self._ble.irq(self._irq)
        ((self._hr_handle,),(self._bg_handle,)) = self._ble.gatts_register_services((_HEART_RATE_SERVICE,
                                                                                     _BLOOD_GLUCOSE_SERVICE))
        self._connections = set()
        self._button = Pin(0, Pin.IN, Pin.PULL_DOWN)
        self._button_state = 0
        self._data_sent = False

        if len(name) == 0:
            name = "Pico"
        self._payload = advertising_payload(
            name=name, services=[_HEART_RATE_UUID, _BLOOD_GLUCOSE_UUID]
        )
        self._advertise()

    def _irq(self, event, data):
        if event == _IRQ_CENTRAL_CONNECT:
            conn_handle, _, _ = data
            self._connections.add(conn_handle)
        elif event == _IRQ_CENTRAL_DISCONNECT:
            conn_handle, _, _ = data
            self._connections.remove(conn_handle)
            self._advertise()

    def check_button(self):
        if self._button.value() == 1 and self._button_state < 2:
            self._button_state += 1
            self._data_sent = False
            time.sleep(0.3)  

    def update_heart_rate(self, notify=False):
        if not self._data_sent and self._button_state == 1:
            heart_rate_value = random.randint(60, 100)
            heart_rate_measurement = struct.pack("<BB", 0, heart_rate_value)
            print("write heart rate: %d bpm" % heart_rate_value)
            self._ble.gatts_write(self._hr_handle, heart_rate_measurement)
            if notify:
                for conn_handle in self._connections:
                    self._ble.gatts_notify(conn_handle, self._hr_handle)
            self._data_sent = True

    def update_blood_glucose(self, notify=False):
        if not self._data_sent and self._button_state == 2:
            glucose_value = random.randint(70, 140)
            glucose_measurement = struct.pack("<H", glucose_value)
            print("write blood glucose: %d mg/dL" % glucose_value)
            self._ble.gatts_write(self._bg_handle, glucose_measurement)
            if notify:
                for conn_handle in self._connections:
                    self._ble.gatts_notify(conn_handle, self._bg_handle)
            self._data_sent = True

    def _advertise(self, interval_us=500000):
        self._ble.gap_advertise(interval_us, adv_data=self._payload)

def demo():
    ble = bluetooth.BLE()
    health_sensor = BLEHealthSensor(ble)
    while True:
        health_sensor.check_button()
        if health_sensor._button_state == 1:
            health_sensor.update_heart_rate(notify=True)
        elif health_sensor._button_state == 2:
            health_sensor.update_blood_glucose(notify=True)
        time.sleep_ms(100)

if __name__ == "__main__":
    demo()

