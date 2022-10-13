$(()=>{
	let scroll = up=>{
		$('.note, .dummy-note').css('bottom', (ix,v)=>{
			return parseInt(v) - (200 * (up?1:-1));
		});
	};
	$('.jsRewindBtn').on('click', ()=>{scroll(true);});
	$('.jsFastForwardBtn').on('click', ()=>{scroll(false);});
});
