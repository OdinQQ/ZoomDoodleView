package com.qtj.zoomdoodleview;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

public abstract class Action {

	public float startX = 0;
	public float startY = 0;
	public float endX = 0;
	public float endY = 0;
	public int color;
	Paint mPaint;

	public void draw(Canvas canvas) {
	}

	public void move(float x, float y) {
	}

	public Action() {
		mPaint = new Paint();
		mPaint.setPathEffect(new DashPathEffect(new float[] { 5, 5, 5 }, 1f));
	}

	/**
	 * 设置偏移
	 * 
	 * @param dx
	 * @param dy
	 */
	public void replace(float dx, float dy) {
		startX += dx;
		startY += dy;
		endX += dx;
		endY += dy;
	}

	/**
	 * 重新设置大小
	 * */
	public void resize(float sx, float sy, float ex, float ey) {
		startX = sx;
		startY = sy;
		endX = ex;
		endY = ey;
	}

	public void finishAdjusting() {
		mPaint.setPathEffect(null);
	}

}
