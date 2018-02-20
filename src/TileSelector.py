#!/usr/bin/python
import gi
gi.require_version('Gtk', '3.0')
from gi.repository import Gtk
from Tiles import *

class TileSelector(Gtk.Window):
	def __init__(self,connection):
		Gtk.Window.__init__(self)
		self.connect("destroy", Gtk.main_quit)
		
		self.grid = Gtk.Grid()
		
		self.add_tile(StraightH,0,0)
		self.add_tile(StraightV,1,0)
		self.add_tile(Diag_TL,0,1)
		self.add_tile(Diag_TR,1,1)
		self.add_tile(Diag_BR,2,1)
		self.add_tile(Diag_BL,3,1)
		self.add_tile(TO_BRL,0,2)
		
		self.add(self.grid)
		
		self.show_all()
		Gtk.main()
		
	def add_tile(self,tile_class,x,y):
		tile = tile_class()
		tile.connect('button-press-event',self.select)
		self.grid.attach(tile,x,y,1,1)
		
	def select(self,btn,evt):
		self.tile = btn.__class__
		self.hide()
		Gtk.main_quit()