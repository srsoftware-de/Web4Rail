PREFIX = $$HOME
SRCPD = 2.1.5
DO = cd $(PREFIX) &&

define srcpd_conf
<?xml version="1.0"?>
<srcpd version="2.0">
  <bus number="0">
    <server>
      <tcp-port>4303</tcp-port>
      <pid-file>/var/run/srcpd.pid</pid-file>
      <username>nobody</username>
      <groupname>nogroup</groupname>
    </server>
    <verbosity>3</verbosity>
  </bus>
  <bus number="1">
    <ddl>
      <enable_nmradcc>yes</enable_nmradcc>
      <enable_maerklin>no</enable_maerklin>
      <enable_usleep_patch>yes</enable_usleep_patch>
    </ddl>
    <use_watchdog>no</use_watchdog>
    <verbosity>3</verbosity>
    <auto_power_on>yes</auto_power_on>
    <device>/dev/serial0</device>
   </bus>
</srcpd>
endef

export srcpd_conf

default: start_all

clean:
	$(DO) rm -r srcpd*

install_all: install_s88 install_w4r install_srcpd

install_srcpd: update_apt
	@echo
	@echo Installiere SRCPD...
	@echo
	sudo apt-get install -y automake libxml2-dev screen
	-$(DO) wget https://sourceforge.net/projects/srcpd/files/srcpd/$(SRCPD)/srcpd-$(SRCPD).tar.gz/download -O srcpd.tgz -nc
	$(DO) tar xfz srcpd.tgz
	$(DO) cd srcpd-?.?.? && ./configure && make && sudo make install
	sudo mv /usr/local/etc/srcpd.conf /usr/local/etc/srcpd.conf.bak-$$(date +%s)
	@echo "$$srcpd_conf" | sudo tee /usr/local/etc/srcpd.conf
	sudo sed -ie "s|nobody|$$USER|g;s|nogroup|$$USER|g" /usr/local/etc/srcpd.conf
	sudo cp /boot/cmdline.txt /boot/cmdline.txt.bak-$$(date +%s)
	sudo sed -i /boot/cmdline.txt -e "s| console=[^ ]*||g"
	@echo
	@echo Starte in 60 Sekunden neu. Danach kann die Software mit den folgenden Befehlen gestartet werden:
	@echo
	@echo make
	@echo
	@echo oder
	@echo
	@echo make start_srcpd
	@echo
	@echo Drücken Sie Strg + C, um den Neustart abzubrechen
	@echo
	sleep 60
	sudo reboot

install_s88: update_apt
	@echo
	@echo Installiere S88 proxy...
	@echo
	sudo apt-get install -y git screen
	-$(DO) git clone -b S88 https://github.com/StephanRichter/model-rail-control

install_w4r: update_apt
	@echo
	@echo Installiere Web4Rail...
	@echo
	sudo apt-get install -y git maven openjdk-11-jdk-headless screen
	-$(DO) git clone https://github.com/srsoftware-de/Web4Rail
	$(DO) cd Web4Rail && mvn clean install

start_all: start_srcpd start_s88 start_w4r

start_srcpd:
	-sudo killall hciattach # kill serial console
	screen -dmS SRCPD sudo /usr/local/sbin/srcpd -n
	sleep 7
	@echo Um sich mit diesem Prozess zu verbinden, führen Sie
	@echo
	@echo screen -R SRCPD
	@echo
	@echo aus.


start_s88:
	$(DO) cd model-rail-control/src && screen -dmS S88 ./sensor-proxy.py
	@echo Um sich mit diesem Prozess zu verbinden, führen Sie
	@echo
	@echo screen -R S88
	@echo
	@echo aus.

start_w4r:
	$(DO) cd Web4Rail && screen -dmS Web4Rail mvn exec:java
	@echo Um sich mit diesem Prozess zu verbinden, führen Sie
	@echo
	@echo screen -R Web4Rail
	@echo
	@echo aus.

stop:
	killall screen


update_apt:
	sudo apt-get update

update: update_apt update_w4r

update_w4r:
	cd Web4Rail && git pull && mvn clean

