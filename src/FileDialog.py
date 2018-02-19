#!/usr/bin/python
import gi
gi.require_version('Gtk', '3.0')
from gi.repository import Gtk

class FileDialog(Gtk.Window):

	def __init__(self,title,hint):
		Gtk.Window.__init__(self, title=title)
		self.connect("delete-event", Gtk.main_quit)

		self.hint = Gtk.Label(hint);
		self.dir_grid = Gtk.Grid()
		self.dir_input = Gtk.Entry();
		self.new_btn = Gtk.Button('Create')
		self.new_btn.connect('clicked',self.new_btn_clck)
		
		self.y = 0
		
		
	def add_dir(self,name):
		btn = Gtk.Button(label=name)
		btn.connect("clicked",self.select_dir)
		self.dir_grid.attach(btn,0,self.y,1,1)
		self.y+=1
		
	def new_btn_clck(self,widget):
		self.dir = self.dir_input.get_text()
		self.hide()
		Gtk.main_quit()
	
	def run(self):
		scroll = Gtk.ScrolledWindow(hexpand=True, vexpand=True)
		
		
		grid = Gtk.Grid()		
		scroll.add(self.dir_grid)
		
		grid.attach(self.hint,		0,0,2,1)
		grid.attach(scroll,			0,1,2,1)
		grid.attach(self.dir_input,	0,2,1,1)
		grid.attach(self.new_btn,	1,2,1,1)
		
		self.add(grid)
		self.set_size_request(	300, 900)
		self.show_all()
		Gtk.main()
		
	def select_dir(self,widget):
		self.dir = widget.get_label()
		self.hide()
		Gtk.main_quit()