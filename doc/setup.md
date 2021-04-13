# Setup

## Systemvoraussetzungen

### Linux

Web4Rail ist in Java programmiert, entsprechend braucht man folgende Programme auf dem Linux-Rechner, um Web4Rail zu bauen und zu starten:

* Git (optional, macht aber den Download einfacher)
* Java (empfohlen: OpenJDK-11-jdk-headless)
* Maven (für das Compilieren)

## Installation

1. Öffne ein Terminal
2. gib ein: `git clone https://github.com/srsoftware-de/Web4Rail.git`
3. wechsle in den heruntergeladenen Ordner und gib ein: `mvn java:exec` und Maven sollte das Programm aus dem Quellcode zusammenbauen und starten.

## Hardware

Das Programm kann auf einem beliebigen Rechner installiert werden. Falls du es auf einem Raspberry-Pi installieren willst, brauchst du wenigstens einen Pi 3.

## Raspberry Pi

Eine ausführliche Anleitung zur Installation auf einem Raspberry Pi 3 findest du unter [srsoftware.de/web4rail-einrichtung](https://srsoftware.de/web4rail-einrichtung)

Auf einem – mit Noops – frisch eingerichteten Raspberry Pi kann man auch wie folgt vorgehen:

1. Anmelden mit Benutername „pi“ und Passwort „raspberry“
2. Download des Installationsskriptes per  
`wget https://github.com/srsoftware-de/Web4Rail/raw/master/install/pi/Makefile`
3. Start der Installation:  
    * Falls nur Web4Rail installiert werden soll: `make install_w4r`
    * Falls Web4Rail zusammen mit SRCPD und dem S88-Proxy installiert werden soll: `make install_all`