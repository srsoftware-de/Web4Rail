#!/usr/bin/python
import gi
gi.require_version('Gtk', '3.0')
from gi.repository import Gtk

class YesNoDialog(Gtk.Window):
	def __init__(self,question):
		Gtk.Window.__init__(self)
		self.connect("delete-event", Gtk.main_quit)

		grid = Gtk.Grid()
		
		question = Gtk.Label(question);
		yes = Gtk.Button('Yes')
		yes.connect('clicked',self.evaluate)
		no = Gtk.Button('No')
		no.connect('clicked',self.evaluate)
		
		grid.attach(question,0,0,2,1)
		grid.attach(yes,0,1,1,1)
		grid.attach(no,1,1,1,1)
		self.add(grid)
		
		self.answer = None
		
		self.show_all()
		Gtk.main()
		
	def evaluate(self,widget):
		self.answer=widget.get_label() == 'Yes'
		self.hide()
		Gtk.main_quit()