package com.fsm.storybook.launcher;

import com.fsm.storybook.model.Highlight;

public interface OnHighlightListener {
	public int saveHighlight(String cfi, String text);
	public int saveHighlight(String cfi, String text, String note);
	public Highlight getHighlight(int id);
	public boolean deleteHighlight(int id);
	
}
