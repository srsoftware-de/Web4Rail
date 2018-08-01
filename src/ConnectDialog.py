#!/usr/bin/python
import gi
gi.require_version('Gtk', '3.0')
from gi.repository import Gtk


class ConnectDialog(Gtk.Window):

	host = None
	port = None

	def __init__(self):
		Gtk.Window.__init__(self, title="Connect to server")
		self.connect("delete-event", Gtk.main_quit)

		grid = Gtk.Grid()
		self.add(grid)

		self.host_label = Gtk.Label('Hostname or IP')

		self.host_input = Gtk.Entry()
		self.host_input.set_text('localhost')

		self.port_label = Gtk.Label('Port')
		self.port_input = Gtk.Entry()
		self.port_input.set_text('7668')

		self.connect_btn = Gtk.Button(label="Connect")
		self.connect_btn.connect("clicked", self.finish)

		self.abort_btn = Gtk.Button(label="Abort")
		self.abort_btn.connect("clicked", self.abort)

		grid.attach(self.host_label, 0, 0, 1, 1)
		grid.attach(self.host_input, 1, 0, 1, 1)
		grid.attach(self.port_label, 0, 1, 1, 1)
		grid.attach(self.port_input, 1, 1, 1, 1)
		grid.attach(self.abort_btn, 0, 2, 1, 1)
		grid.attach(self.connect_btn, 1, 2, 1, 1)

	def abort(self, widget):
		Gtk.main_quit()

	def run(self):
		self.show_all()
		Gtk.main()

	def finish(self, widget):
		self.host = self.host_input.get_text()
		self.port = int(self.port_input.get_text())
		self.hide()
		Gtk.main_quit()