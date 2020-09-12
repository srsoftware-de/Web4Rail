const ADD = 1;
const SQUARE = 30;
const BODY = 'body';
const SVG = 'svg';
var selected = null;
var mode = null;


function addTile(tile,x,y){	
	console.log("addTile:",tile.id,x,y);
	x = Math.floor(x/SQUARE);
	y = Math.floor(y/SQUARE);
	var id = 'tile-'+x+'-'+y;
	$('#'+id).remove();
	var tile = $(tile).clone().css({left:(30*x)+'px',top:(30*y)+'px','border':''}).attr('id',id);
	console.log(tile);
	$(BODY).append(tile);
}

function bodyClick(ev){
	console.log('bodyClick:',ev);
	switch (mode){
	case undefined:
		case null: return;
		case ADD:
			return addTile(selected,ev.clientX,ev.clientY);
	}
	console.log(ev.clientX,ev.clientY);
}

function closeMenu(ev){
	console.log('closeMenu:',ev);
	if (selected != null) $(selected).css('border','');
	$('.menu .list').css('display','')
	mode = null;
	selected = null;
	return false;
}

function enableAdding(ev){
	console.log('enableAdding:',ev);
	if (selected != null) $(selected).css('border','');
	selected = ev.target;
	while (selected != null && selected.nodeName != SVG) selected = selected.parentNode;
	if (selected == null){
		mode = null;
	} else {
		$(selected).css('border','2px solid red');
		$('.menu .tile .list').css('display','inherit');
		mode = ADD;
	}
	return false; // otherwise body.click would also be triggered
}

window.onload = function () {
	var isDragging = false;
	$('.menu > div').click(closeMenu);
	$('.menu .tile .list svg').click(enableAdding);
	$(BODY).click(bodyClick);
}
