#!/usr/bin/python
import gi
gi.require_version('Gtk', '3.0')
from gi.repository import Gtk
from TileSelector import *
import types
import os

def dump_obj(obj, key='',level=0):
	for key, value in obj.__dict__.items():
		if isinstance(value, (int, float, str, unicode, list, dict, set)):
			 print " " * level + "%s -> %s" % (key, value)
		else:
			print " " * level + "%s -> %s:" % (key, value.__class__.__name__)
			dump_obj(value, key, level + 2)



class TrackPlan(Gtk.Window):
	def __init__(self,json):
		Gtk.Window.__init__(self)
		self.connect("delete-event", Gtk.main_quit)
	
		self.grid = Gtk.Grid()
		
		self.putButton(0,0,None,None)
		
		self.add(self.grid)
		

		
	def select_tile(self,widget,origin,connection):
		#print 'Button at ({},{}) pressed'.format(widget.x, widget.y)
		tileSelector = TileSelector(connection)
		tile = tileSelector.tile()
		if origin != None:
			if connection == Tile.TOP:
				origin.bottom = tile
			if connection == Tile.RIGHT:
				origin.left = tile
			if connection == Tile.BOTTOM:
				origin.top = tile
			if connection == Tile.LEFT:
				origin.right = tile
		self.putTile(tile,widget.x,widget.y)
		widget.destroy()
		
	def putButton(self,x,y,origin,connection):
		#print 'putButton({},{})'.format(x,y)
		btn = Gtk.Button('?')
		btn.x = x
		btn.y = y
		btn.connect('clicked',self.select_tile,origin,connection)
		self.grid.attach(btn,x,y,1,1)
		btn.show()		
		
	def putTile(self,tile,x,y):
		#print 'putTile({},{})'.format(x,y)
		tile.x = x
		tile.y = y
		self.grid.attach(tile,x,y,1,1)
		tile.show()
		
		if tile.connects_left():
			left = self.grid.get_child_at(x-1,y)
			if left == None:
				self.putButton(x-1,y,tile,Tile.RIGHT)
		if tile.connects_right():
			right = self.grid.get_child_at(x+1,y)
			if right == None:
				self.putButton(x+1,y,tile,Tile.LEFT)
		if tile.connects_up():
			top = self.grid.get_child_at(x,y-1)
			if top == None:
				self.putButton(x,y-1,tile,Tile.BOTTOM)
		if tile.connects_down():
			bottom = self.grid.get_child_at(x,y+1)
			if bottom == None:
				self.putButton(x,y+1,tile,Tile.TOP)
		
		self.save()
		
	def run(self):
		self.show_all()
		Gtk.main()
		
	def save(self):
		seed_tile = self.grid.get_child_at(0,0)
		os.system('clear') 
		print seed_tile.json()