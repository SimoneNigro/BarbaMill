package it.unibo.ai.didattica.mulino.actions;

import it.unibo.ai.didattica.mulino.domain.BitBoardUtil;

public class ByteAction{
	
	public byte put;
	public byte from;
	public byte to;
	public byte remove;
	public byte phase;

	public ByteAction(byte phase){
		this.put = 99;
		this.from = 99;
		this.to = 99;
		this.remove = 99;
		this.phase = phase;
	}
	public ByteAction(){
		
		this.put = 99;
		this.from = 99;
		this.to = 99;
		this.remove = 99;
		this.phase = 99;
	}

	public String toString(){
		switch(phase){
		
		case 1:
			return BitBoardUtil.byteToStr(this.put)+((remove == 99) ? "" : BitBoardUtil.byteToStr(remove));
		case 2:
			return BitBoardUtil.byteToStr(from)+BitBoardUtil.byteToStr(to)+
					((remove == 99) ? "" : BitBoardUtil.byteToStr(remove));
		case 3: 
			return BitBoardUtil.byteToStr(from)+BitBoardUtil.byteToStr(to)+
					((remove == 99) ? "" : BitBoardUtil.byteToStr(remove));
		case 99:
			return "Non inizializzata";
		}
		return null;
	}
	
}
