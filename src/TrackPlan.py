#!/usr/bin/python
import gi
gi.require_version('Gtk', '3.0')
from gi.repository import Gtk
from TileSelector import *

class TrackPlan(Gtk.Window):
	def __init__(self,json):
		Gtk.Window.__init__(self)
		self.connect("delete-event", Gtk.main_quit)
	
		self.grid = Gtk.Grid()
		
		self.putButton(0,0,None)
		
		self.add(self.grid)
		
		self.show_all()
		Gtk.main()
		
	def select_tile(self,widget,connections):
		print 'Button at ({},{}) pressed'.format(widget.x, widget.y)
		tileSelector = TileSelector(connections)
		tile = tileSelector.tile()
		self.putTile(tile,widget.x,widget.y)
		widget.destroy()
		
	def putButton(self,x,y,connections):
		print 'putButton({},{})'.format(x,y)
		btn = Gtk.Button('?')
		btn.x = x
		btn.y = y
		btn.connect('clicked',self.select_tile,connections)
		self.grid.attach(btn,x,y,1,1)
		btn.show()		
		
	def putTile(self,tile,x,y):
		print 'putTile({},{})'.format(x,y)
		tile.x = x
		tile.y = y
		self.grid.attach(tile,x,y,1,1)
		tile.show()
		
		if tile.connects_left():
			left = self.grid.get_child_at(x-1,y)
			if left == None:
				self.putButton(x-1,y,tile.connections())
		if tile.connects_right():
			right = self.grid.get_child_at(x+1,y)
			if right == None:
				self.putButton(x+1,y,tile.connections())
		if tile.connects_up():
			top = self.grid.get_child_at(x,y-1)
			if top == None:
				self.putButton(x,y-1,tile.connections())
		if tile.connects_down():
			bottom = self.grid.get_child_at(x,y+1)
			if bottom == None:
				self.putButton(x,y+1,tile.connections())
			