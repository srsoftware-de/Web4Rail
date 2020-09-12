const ADD = 'add';
const SQUARE = 30;
const BODY = 'body';
const SVG = 'svg';
const PLAN = 'plan';
const POST = 'POST';
var selected = null;
var mode = null;

function addMessage(txt){
	$('#messages').html(txt).show().delay(1000).fadeOut();
}

function addTile(x,y){	
	console.log("addTile:",selected.id,x,y);
	x = Math.floor(x/SQUARE);
	y = Math.floor(y/SQUARE);
	$.ajax({
		url : PLAN,
		method: POST,
		data : {action:mode,tile:selected.id,x:x,y:y},
		success: function(resp){
			var id = 'tile-'+x+'-'+y;
			$('#'+id).remove();
			var tile = $(selected).clone().css({left:(30*x)+'px',top:(30*y)+'px','border':''}).attr('id',id);
			if (selected.id != 'Eraser') $(BODY).append(tile);
			addMessage(resp);
		}
	});

}

function bodyClick(ev){
	//console.log('bodyClick:',ev);
	switch (mode){
		case undefined:
		case null:
			return clickTile(ev.clientX,ev.clientY);
		case ADD:
			return addTile(ev.clientX,ev.clientY);
	}
	console.log(ev.clientX,ev.clientY);
}

function clickTile(x,y){
	console.log("clickTile:",x,y);
	x = Math.floor(x/SQUARE);
	y = Math.floor(y/SQUARE);
	if ($('#tile-'+x+'-'+y).length > 0){
		$.ajax({
			url : PLAN,
			method : POST,
			data : {action:'openProps',x:x,y:y},
			success: function(resp){
				$('body').append($(resp));
			}
		});
	}
	return false;
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
//	console.log('enableAdding:',ev);
	if (selected != null) $(selected).css('border','');
	selected = ev.target;
	while (selected != null && selected.nodeName != SVG) selected = selected.parentNode;
	if (selected == null){
		mode = null;
	} else {
		$(selected).css('border','2px solid red');
		$('.menu .addtile .list').css('display','inherit');
		mode = ADD;
	}
	return false; // otherwise body.click would also be triggered
}

function savePlan(ev){
	$.ajax({
		url : PLAN,
		method : POST,
		data : {action:'save',name:'default'}, // todo: ask for name
		success: function(resp){ addMessage(resp);}
	});
	return false;
}

window.onload = function () {
	var isDragging = false;
	$('.menu > div').click(closeMenu);
	$('.menu .addtile .list svg').click(enableAdding);
	$(BODY).click(bodyClick);
	$('#save').click(savePlan);
}
