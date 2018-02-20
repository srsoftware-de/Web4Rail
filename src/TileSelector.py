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
		
		x0=0
		x1=0
		x2=0
		
		if connection in {Tile.LEFT,Tile.RIGHT,None}:
			self.add_tile(StraightH,x0,0)
			x0+=1
			
		if connection in {Tile.TOP,Tile.BOTTOM,None}:
			self.add_tile(StraightV,x0,0)
			x0+=1
			
		if connection in {Tile.TOP,Tile.LEFT,None}:
			self.add_tile(Diag_TL,x1,1)
			x1+=1
			
		if connection in {Tile.TOP,Tile.RIGHT,None}:
			self.add_tile(Diag_TR,x1,1)
			x1+=1
			
		if connection in {Tile.BOTTOM,Tile.RIGHT,None}:
			self.add_tile(Diag_BR,x1,1)
			x1+=1
			
		if connection in {Tile.BOTTOM,Tile.LEFT,None}:
			self.add_tile(Diag_BL,x1,1)
			x1+=1
			
		if connection in {Tile.BOTTOM,Tile.LEFT,Tile.RIGHT,None}:
			self.add_tile(TO_BRL,0,2)
			self.add_tile(TO_BLR,1,2)
			
		if connection in {Tile.TOP,Tile.LEFT,Tile.RIGHT,None}:
			self.add_tile(TO_TRL,0,3)
			self.add_tile(TO_TLR,1,3)
		
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