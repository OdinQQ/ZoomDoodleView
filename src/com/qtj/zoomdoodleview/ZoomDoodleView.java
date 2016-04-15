package com.qtj.zoomdoodleview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * 可缩放 涂鸦View
 */
public class ZoomDoodleView extends View {

	/** 模式 */
	public abstract class Mode {
		public static final int NONE_MODE = 0;
		public static final int LINE_MODE = 1;
		public static final int RECT_MODE = 2;
		public static final int CIRCLE_MODE = 3;
		public static final int ERASER_MODE = 4;
		public static final int DEEP_ERASER_MODE = 5;
		public static final int TEXT_MODE = 6;
	}

	/** 背景Bitmap */
	private Bitmap background = null;

	/** 实际绘画的Bitmap */
	private Bitmap surfaceBitmap = null;

	// private boolean ifLoop = true; // 绘画线程结束标志位
	private LinkedList<Action> actionList = null; // 绘画动作的列表
	// 正在进行的动作
	private Action curAction = null;
	// private SurfaceHolder holder = null;
	// private Paint paint = null;

	private static final int DEFAULT_TEXT_COLOR = Color.BLACK;
	private static final int DEFAULT_LINE_COLOR = Color.BLACK;
	private static final int DEFAULT_LINE_SIZE = 5;
	private static final int DEFAULT_TEXT_SIZE = 35;

	/** 当前Action的类型 */
	private int curMode = 0;
	/** 当前Action在actionList中的index，用于前进后退 */
	private int curIndex = 0;
	// 画笔的颜色
	private int color = DEFAULT_TEXT_COLOR;
	// 画笔的大小
	private float size = DEFAULT_LINE_SIZE;
	/** 背景色 */
	private int backgroundColor = Color.GRAY;
	/** 默认模式 */
	private static final int DEFAULT_DRAW_MODE = Mode.LINE_MODE;

	Handler mHandler = new Handler();

	private int mCurAdjustState;
	/** 当前运行的线程 */
	private Runnable mCurRunnable;
	private static final int NOT_DRAGING = 0;
	private static final int MOVING = 9;
	private static final int MOVING_FINISHED = 10;
	private static final int RESIZING = 11;
	private static final int DRAGING = 12;
	private static final int ADJUSTING_DELAY = 1000;
	private static final int DRAGING_DELEGATE = 5;
	private float dragStartX;
	private float dragStartY;
	private float dragCurX;
	private float dragCurY;
	// 拖动位置变化
	private float dragActionStartX;
	private float dragActionStartY;
	private float dragActionEndX;
	private float dragActionEndY;
	private float dragOriLen;
	private float originTextSize;

	/** 当前的画布模式 */
	private CanvasMode curCanvasMode = CanvasMode.NONE;

	/**
	 * 画布模式
	 */
	private enum CanvasMode {
		NONE, DRAG, ZOOM
	}

	// 点
	private PointF startPoint = new PointF();
	private PointF endPoint = new PointF();
	private PointF midPoint = new PointF();
	private float oldDis = 0;
	private float newDis = 0;
	/** 是否处于缩放和拖动模式 */
	private boolean ifEnableDragAndZoom = false;

	private static final float POINTER_DIS_LIMIT_MIN = 230;
	private static final float POINTER_DIS_LIMIT_MAX = 280;
	// 缩放
	private float scale = 1;
	// 全部缩放比例
	private float totalScale = 1;
	// Matrix
	private Matrix tempMatrix = new Matrix();
	private Matrix saveMatrix = new Matrix();
	private float[] m = new float[9];
	// 平移
	private float xTranslateDis = 0;
	private float yTranslateDis = 0;
	//
	private float xMoveDis = 0;
	private float yMoveDis = 0;

	// private static final int MAX_SIZE = 10;
	private static final String LOG_TAG = "Ragnarok";

	private int backgroundOriginWidth = 0;
	private int backgroundOriginHeight = 0;
	/** 默认画布宽高 */
	private int DEFAULT_WIDTH = 600, DEFAULT_HEIGHT = 600;

	private int canvasWidth = DEFAULT_WIDTH;
	private int canvasHeight = DEFAULT_HEIGHT;

	private Context mContext;

	private Rect viewRect = new Rect();

	public ZoomDoodleView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		ini(context);
	}

	public ZoomDoodleView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
		ini(context);
	}

	public ZoomDoodleView(Context context) {
		this(context, null, 0);
		ini(context);
	}

	/**
	 * 构造初始化
	 * 
	 * @param context
	 */
	private void ini(Context context) {
		actionList = new LinkedList<Action>();
		this.mContext = context;
		this.curIndex = actionList.size();
		this.setFocusable(true);
		ifEnableDragAndZoom = false;
		scale = 1;
		curMode = DEFAULT_DRAW_MODE;
		// 绘图区域
		// surfaceBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight,
		// Bitmap.Config.ARGB_8888);

	}

	/**
	 * 设置画布Bitmap的大小
	 */
	public void setCanvasSize(int width, int height) {
		this.canvasWidth = width;
		this.canvasHeight = height;
		// recycleBitmap(surfaceBitmap);
		surfaceBitmap = Bitmap.createBitmap(canvasWidth, canvasHeight,
				Bitmap.Config.ARGB_8888);
	}

	public void setMode(int mode) {
		this.curMode = mode;
	}

	public void setColor(int color) {
		this.color = color;
	}

	/**
	 * 是否是拖动缩放模式
	 * 
	 * @param ifEnableDragAndZoom
	 */
	public void setEnableDragAndZoom(boolean ifEnableDragAndZoom) {
		this.ifEnableDragAndZoom = ifEnableDragAndZoom;
	}

	public void setBrushSize(float size) {
		this.size = size;
	}

	/**
	 * 设置背景Bitmap
	 * 
	 * @param background
	 */
	public void setBackground(Bitmap background) {
		this.background = background;
		this.canvasWidth = background.getWidth();
		this.canvasHeight = background.getHeight();
		if (background != null) {
			// recycleBitmap(surfaceBitmap);
			this.surfaceBitmap = Bitmap.createBitmap(background.getWidth(),
					background.getHeight(), Bitmap.Config.ARGB_8888);
		}
	}

	/**
	 * 回收Bitmap
	 * 
	 * @param bitmap
	 */
	private void recycleBitmap(Bitmap bitmap) {
		if (bitmap != null && !bitmap.isRecycled()) {
			bitmap.recycle();
			bitmap = null;
		}
	}

	/**
	 * 清空画面
	 */
	public void clear() {
		this.actionList.clear();
		this.curIndex = this.actionList.size();
		invalidate();
	}

	/**
	 * 添加Action到集合中
	 * 
	 * @param action
	 */
	private void addAction(Action action) {
		actionList.add(action);
		curIndex = curIndex + 1 > actionList.size() ? actionList.size()
				: curIndex + 1;
	}

	/**
	 * 将图片保存到本地
	 * 
	 * @param path
	 * @return
	 */
	public boolean saveToFile(String path) {
		File file = new File(path);
		if (!file.exists()) {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (surfaceBitmap != null) {
			return surfaceBitmap.compress(CompressFormat.PNG, 100, fos);
		}
		return false;
	}

	/**
	 * 回滚
	 */
	public void rollBack() {
		curIndex = curIndex - 1 < 0 ? 0 : curIndex - 1;
		if (actionList.size() > 0)
			actionList.removeLast();
		invalidate();
	}

	/**
	 * 画面是否为空
	 * 
	 * @return
	 */
	public boolean ifClear() {
		return actionList.size() > 0;
	}

	/**
	 * 撤销
	 */
	public void goForward() {
		curIndex = curIndex + 1 > actionList.size() ? actionList.size()
				: curIndex + 1;
		invalidate();
	}

	/**
	 * 绘制
	 */
	public void draw(Canvas canvas) {

		// 底色
		canvas.drawColor(backgroundColor);
		// 设置画布坐标变化
		// canvas.setMatrix(saveMatrix);
		// 创建surface画布，在上面绘制Action
		Canvas surfaceCanvas = new Canvas(surfaceBitmap);
		// surfaceCanvas.translate((getWidth() - canvasWidth) / 2,
		// (getHeight() - canvasHeight) / 2);
		// 绘制背景
		if (background != null) {
			surfaceCanvas.drawBitmap(background, 0, 0, null);
		} else {
			surfaceCanvas.drawColor(Color.WHITE);
		}
		// 绘制Action
		for (int i = 0; i < this.curIndex; i++) {
			this.actionList.get(i).draw(surfaceCanvas);
		}
		if (this.curAction != null)
			curAction.draw(surfaceCanvas);
		// 在画布上绘制surfaceBitmap
		canvas.drawBitmap(surfaceBitmap, saveMatrix, null);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		this.canvasWidth = MeasureSpec.getSize(widthMeasureSpec);
		this.canvasHeight = MeasureSpec.getSize(heightMeasureSpec);
		setCanvasSize(canvasWidth, canvasHeight);
	}

	/**
	 * 设置当前Action
	 * 
	 * @param x
	 * @param y
	 * @param size
	 * @param color
	 */
	private void setCurAction(float x, float y, float size, int color) {
		if (curAction != null) {
			mHandler.removeCallbacks(mRunnable);
			mRunnable.run();
		}
		switch (this.curMode) {
		case ZoomDoodleView.Mode.CIRCLE_MODE:
			curAction = new CircleAction(x, y, size, color);
			break;
		case ZoomDoodleView.Mode.LINE_MODE:
			curAction = new LineAction(x, y, size, color);
			Log.d(LOG_TAG, "curAction is line mode");
			break;
		case ZoomDoodleView.Mode.RECT_MODE:
			curAction = new RectAction(x, y, size, color);
			break;
		case ZoomDoodleView.Mode.ERASER_MODE:
			curAction = new EraserAction(x, y, size, color);
			break;
		case ZoomDoodleView.Mode.DEEP_ERASER_MODE:
			curAction = new DeepEraserAction(x, y, size, this.backgroundColor);
			break;
		default:
			curAction = null;
			Log.d(LOG_TAG, "curAction is null");
		}
	}

	// //////////// 触摸事件 ////////////////////
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_CANCEL) {
			return false;
		}
		float x = event.getX();
		float y = event.getY();
		// 转换坐标
		saveMatrix.getValues(m);
		float drawX = (x - m[Matrix.MTRANS_X]) / m[Matrix.MSCALE_X];
		float drawY = (y - m[Matrix.MTRANS_Y]) / m[Matrix.MSCALE_Y];

		if (mCurRunnable != null) { // 操作Action对象

			// 单指
			if (event.getPointerCount() == 1) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
				case MotionEvent.ACTION_POINTER_DOWN:
					mHandler.removeCallbacks(mRunnable);
					if (mCurAdjustState == RESIZING) {
						return true;
					}
					// mCurAdjustState = DRAGING;
					dragCurX = dragActionStartX = drawX;
					dragCurY = dragActionStartY = drawY;
					break;
				case MotionEvent.ACTION_MOVE:
					if (mCurAdjustState == RESIZING) {
						return true;
					} else if (mCurAdjustState != DRAGING
							&& (Math.abs(drawX - dragActionStartX) > DRAGING_DELEGATE || Math
									.abs(drawY - dragActionStartY) > DRAGING_DELEGATE)) {
						mCurAdjustState = DRAGING;
					} else if (mCurAdjustState != DRAGING) {
						return true;
					}
					curAction.replace(drawX - dragCurX, drawY - dragCurY);
					dragCurX = drawX;
					dragCurY = drawY;
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
					// if( mCurAdjustState==DRAGING )
					// curAction.replace(drawX - dragCurX, drawY - dragCurY);
					mCurAdjustState = 0;
					cancelLongPress();
					mHandler.postDelayed(mRunnable, ADJUSTING_DELAY);
					break;
				}
			} else if (event.getPointerCount() == 2) { // 双点操作
				switch (event.getAction()) {
				case MotionEvent.ACTION_POINTER_DOWN:
					mCurAdjustState = RESIZING;
					float x0 = event.getX(0);
					float x1 = event.getX(1);
					float y0 = event.getY(0);
					float y1 = event.getY(1);
					dragStartX = drawX;
					dragStartY = drawY;
					dragActionStartX = curAction.startX;
					dragActionStartY = curAction.startY;
					dragActionEndX = curAction.endX;
					dragActionEndY = curAction.endY;
					dragOriLen = getDistance(x0, y0, x1, y1);
					break;
				case MotionEvent.ACTION_MOVE:
					if (mCurAdjustState != RESIZING) {
						mCurAdjustState = RESIZING;
						x0 = event.getX(0);
						x1 = event.getX(1);
						y0 = event.getY(0);
						y1 = event.getY(1);
						dragStartX = drawX;
						dragStartY = drawY;
						dragActionStartX = curAction.startX;
						dragActionStartY = curAction.startY;
						dragActionEndX = curAction.endX;
						dragActionEndY = curAction.endY;
						dragOriLen = getDistance(x0, y0, x1, y1);
						if (curAction instanceof TextAction) {
							originTextSize = ((TextAction) curAction)
									.getTextSize();
						}
					} else {
						x0 = event.getX(0);
						x1 = event.getX(1);
						y0 = event.getY(0);
						y1 = event.getY(1);
						float curLen = getDistance(x0, y0, x1, y1);
						float scale = curLen / dragOriLen;
						scaleAction(scale);
					}
					break;
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
					cancelLongPress();
					// curAction.replace(drawX - dragCurX, drawY - dragCurY);
					mCurAdjustState = 0;
					mHandler.postDelayed(mRunnable, ADJUSTING_DELAY);
				default:
					break;
				}
			} else {
				Log.e(LOG_TAG, "Too many pointer..");
				cancelLongPress();
			}
			invalidate();
			return true;
		} else { // Runnable == null 操作画布
			if (event.getPointerCount() == 1) { // 单指
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					this.setCurAction(drawX, drawY, this.size, this.color);
					// Log.d(LOG_TAG,
					// "action down, pointer count is "
					// + event.getPointerCount());
					mCurAdjustState = MOVING;
					// 缩放和拖动
					if (this.ifEnableDragAndZoom) {
						this.curCanvasMode = CanvasMode.DRAG;
						startPoint.set(x, y);
						// 缓存Matrix
						tempMatrix.set(saveMatrix);
					}
					break;
				case MotionEvent.ACTION_MOVE:
					if (curAction != null) {
						curAction.move(drawX, drawY);
						// Log.d(LOG_TAG, "move to " + x + ", " + y);
					} else {
						if (this.curCanvasMode == CanvasMode.DRAG) {
							// 平移量
							xTranslateDis = x - startPoint.x;
							yTranslateDis = y - startPoint.y;
							Log.d(LOG_TAG, "xTranslateDis=" + xTranslateDis
									+ ", yTranslateDis=" + yTranslateDis);
							// 恢复之前的数值
							saveMatrix.set(tempMatrix);
							// 平移
							saveMatrix.postTranslate(xTranslateDis,
									yTranslateDis);
						}
					}
					break;
				case MotionEvent.ACTION_UP:
					if (curAction == null)
						break;
					curAction.move(drawX, drawY);
					// 画矩形或画圆Action
					if (curAction instanceof RectAction
							|| curAction instanceof CircleAction) {
						mCurRunnable = mRunnable;
						mHandler.postDelayed(mRunnable, ADJUSTING_DELAY);
					} else {
						addToActionList();
					}
					break;
				}
				invalidate();
				return true;
			} else if (event.getPointerCount() >= 2 && ifEnableDragAndZoom) {
				// 双指 并且处于可缩放拖动状态
				switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
					if (this.curCanvasMode == CanvasMode.DRAG) {
						xMoveDis = xMoveDis + xTranslateDis;
						yMoveDis = yMoveDis + yTranslateDis;
						Log.d(LOG_TAG, "xMoveDis = " + xMoveDis
								+ ", yMoveDis = " + yMoveDis);
					} else if (this.curCanvasMode == CanvasMode.ZOOM) {
						totalScale = totalScale * scale;
						Log.d(LOG_TAG, "totalScale = " + totalScale);
					}
					this.curCanvasMode = CanvasMode.NONE;
					break;
				case MotionEvent.ACTION_POINTER_DOWN:
					Log.d(LOG_TAG, "action_pointer_down");
					oldDis = getDistance(
							new PointF(event.getX(0), event.getY(0)),
							new PointF(event.getX(1), event.getY(1)));
					Log.d(LOG_TAG, "oldDis is " + oldDis);
					if (POINTER_DIS_LIMIT_MIN < oldDis
							&& oldDis > POINTER_DIS_LIMIT_MAX) {
						this.curCanvasMode = CanvasMode.ZOOM;
						Log.d(LOG_TAG, "set to zoom mode");
						// 缓存Matrix
						tempMatrix.set(saveMatrix);
						midPoint = getMidPoint(midPoint, new PointF(x, y));
					}
					break;
				case MotionEvent.ACTION_MOVE:
					if (this.curCanvasMode == CanvasMode.DRAG) { // 平移
						// Log.d(LOG_TAG, "drag the view");
						// 平移量
						xTranslateDis = x - startPoint.x;
						yTranslateDis = y - startPoint.y;
						Log.d(LOG_TAG, "xTranslateDis=" + xTranslateDis
								+ ", yTranslateDis=" + yTranslateDis);
						// 恢复之前的数值
						saveMatrix.set(tempMatrix);
						// 平移
						saveMatrix.postTranslate(xTranslateDis, yTranslateDis);
					} else if (this.curCanvasMode == CanvasMode.ZOOM) { // 缩放
						newDis = getDistance(
								new PointF(event.getX(0), event.getY(0)),
								new PointF(event.getX(1), event.getY(1)));
						Log.d(LOG_TAG, "newDis is " + newDis);
						if (POINTER_DIS_LIMIT_MIN < newDis
								&& newDis > POINTER_DIS_LIMIT_MAX) {
							scale = newDis / oldDis;
							// 恢复之前的数值
							saveMatrix.set(tempMatrix);
							// 缩放
							saveMatrix.postScale(scale, scale, midPoint.x,
									midPoint.y);
							Log.d(LOG_TAG, "scale is " + scale);
						}
					}
					break;
				}
				invalidate();
				return true;
			}
		}
		return super.onTouchEvent(event);
	}

	/**
	 * 线程
	 */
	private Runnable mRunnable = new Runnable() {
		@Override
		public void run() {
			addToActionList();
			cancelLongPress();
			mCurRunnable = null;
		}
	};

	/**
	 * 添加当前Action到集合
	 */
	private void addToActionList() {
		if (curAction != null) {
			// Log.d(LOG_TAG, "move to " + x + ", " + y);
			curAction.finishAdjusting();
			this.addAction(curAction);
			curAction = null;
			mCurAdjustState = 0;
			invalidate();
		}
	}

	private PointF getDrawPoint(float x, float y, float xMoveDis,
			float yMoveDis, PointF zoomMidPoint, float scale) {
		float drawX = x - xMoveDis;
		float drawY = y - yMoveDis;
		float dis = getDistance(new PointF(x, y), zoomMidPoint);
		Log.d("dis", "dis = " + dis);
		float scaleDis = dis * scale;
		Log.d("dis", "scaleDis = " + scaleDis);
		// if (scale != 1) {
		// drawX = drawX + scaleDis;
		// drawY = drawY + scaleDis;
		// }
		Log.d("dis", "zoomMidPoint.x = " + zoomMidPoint.x
				+ ", zoomMidPoint.y = " + zoomMidPoint.y);
		return new PointF(drawX, drawY);
	}

	/**
	 * 获得两点距离
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
	private float getDistance(PointF p1, PointF p2) {
		float xDis = Math.abs(p1.x - p2.x);
		float yDis = Math.abs(p2.y - p1.y);
		return (float) Math.sqrt(xDis * xDis + yDis * yDis);
	}

	/**
	 * 获得两点距离
	 * 
	 * @param x0
	 * @param y0
	 * @param x1
	 * @param y1
	 * @return
	 */
	private float getDistance(float x0, float y0, float x1, float y1) {
		return (float) Math.sqrt((x0 - x1) * (x0 - x1) + (y0 - y1) * (y0 - y1));
	}

	/**
	 * 缩放Action
	 * 
	 * @param scale
	 */
	private void scaleAction(float scale) {
		if (curAction instanceof TextAction) {
			((TextAction) curAction).setTextSize(originTextSize * scale);
		} else {
			// Y轴变化
			float left = dragActionEndY - dragActionStartY;
			// X轴变化
			float up = dragActionEndX - dragActionStartX;
			left = left * (1 - scale);
			up = up * (1 - scale);
			float sx = dragActionStartX + up;
			float ex = dragActionEndX - up;
			float sy = dragActionStartY + left;
			float ey = dragActionEndY - left;
			curAction.resize(sx, sy, ex, ey);
		}
	}

	/**
	 * 获得中点
	 * 
	 * @param p1
	 * @param p2
	 * @return
	 */
	private PointF getMidPoint(PointF p1, PointF p2) {
		return new PointF((p1.x + p2.x) / 2f, (p1.y + p2.y) / 2f);
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.save();
		draw(canvas);
		canvas.restore();
	}

	/**
	 * 添加Text Action
	 * 
	 * @param text
	 */
	public void addTextAction(String text) {
		curAction = new TextAction(text, DEFAULT_TEXT_SIZE, DEFAULT_TEXT_COLOR,
				getWidth() / 2, getHeight() / 2);
		invalidate();
		mHandler.postDelayed(mRunnable, ADJUSTING_DELAY * 2);
		mCurAdjustState = 0;
		mCurRunnable = mRunnable;
	}

}
