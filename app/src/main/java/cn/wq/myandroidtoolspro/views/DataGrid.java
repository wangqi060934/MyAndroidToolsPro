package cn.wq.myandroidtoolspro.views;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import cn.wq.myandroidtoolspro.R;
import cn.wq.myandroidtoolspro.helper.Utils;

public class DataGrid extends View{
	private int INDEX_WIDTH = 25;
	private int CELL_HEIGHT = 22;
	private int column_num, row_num;
	private Rect headerRect = new Rect();
	private Rect leftRect = new Rect();
	private Rect dataRect = new Rect();
	private Paint headerPaint, textPaint, borderPaint,
			seperatorPaint, selectedPaint;
//    private Paint blackPaint;
	private float offsetX, offsetY, maxOffsetX, maxOffsetY;
	private GestureDetectorCompat mGestureDetector;
	private Scroller mScroller;
	private int textPadding = 6;
	private int textSize = 14;
	private int scrollbarSize = 4;
	private int maxCellWidth = 200;
	/**
	 * key=0为title
	 */
	private SparseArray<ArrayList<String>> data;
	private List<Integer> widthList;
	private float hRate, vRate;
	private float hScrollbarLength, vScrollbarLength;
	private int screenWidth, screenHeight;
	private int allDataHeight;
	// 都从0开始
	private int selectedRow = -1;

	public DataGrid(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		setWillNotDraw(false);

		DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
		CELL_HEIGHT = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, CELL_HEIGHT, displayMetrics);
		INDEX_WIDTH = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, INDEX_WIDTH, displayMetrics);
		textSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
				textSize, displayMetrics);
		textPadding = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, textPadding, displayMetrics);
		scrollbarSize = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, scrollbarSize, displayMetrics);
		maxCellWidth = (int) TypedValue.applyDimension(
				TypedValue.COMPLEX_UNIT_DIP, maxCellWidth, displayMetrics);

		headerPaint = new Paint();
		headerPaint.setColor(context.getResources().getColor(
                R.color.actionbar_color_light));

//        TypedValue typedValue=new TypedValue();
//        context.getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
//        final int textColorPrimary=context.getResources().getColor(typedValue.resourceId);
		final int textColorPrimary = Utils.getColorFromAttr(context,android.R.attr.textColorPrimary);

		textPaint = new Paint();
		textPaint.setColor(textColorPrimary);
		textPaint.setTextSize(textSize);

		borderPaint = new Paint();
		borderPaint.setColor(Color.GRAY);
		borderPaint.setStyle(Paint.Style.STROKE);

//		blackPaint = new Paint();
//		blackPaint.setColor(Color.BLACK);

		seperatorPaint = new Paint();
		seperatorPaint.setColor(Color.GRAY);
		seperatorPaint.setStyle(Paint.Style.FILL);

		selectedPaint = new Paint();
//		selectedPaint.setColor(getResources().getColor(R.color.actionbar_color));
//		context.getTheme().resolveAttribute(R.attr.colorPrimary, typedValue, true);
//		final int actionBarColor=context.getResources().getColor(typedValue.resourceId);
		final int actionBarColor = Utils.getColorFromAttr(context, R.attr.colorPrimary);
		selectedPaint.setColor(actionBarColor);

//		mGestureDetector = new GestureDetector(context, this);
		mGestureDetector=new GestureDetectorCompat(context, mGestureListener);
		mScroller = new Scroller(context);

//		setBackgroundColor(Color.BLACK);
	}

	public DataGrid(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DataGrid(Context context) {
		this(context, null);
	}

	public void setData(SparseArray<ArrayList<String>> data,
			List<Integer> widthList) {
		this.data = data;
		this.widthList = widthList;
		column_num = widthList.size();
		row_num = data.size();
		
		if (row_num > 1) {
			final int measuredMaxWidth = (int) (textPaint.measureText(row_num
					+ "") + textPadding);
			if (measuredMaxWidth > INDEX_WIDTH) {
				INDEX_WIDTH = measuredMaxWidth;
			}
		} else if (row_num == 1) {
			INDEX_WIDTH = 0;
		}

		offsetX=0;
		offsetY=0;
		selectedRow=-1;
		if(onSelectChangedListener!=null){
			onSelectChangedListener.onSelectChanged(0, null);
		}
		
		resize();
	}
	
	public void updateLine(int row,ArrayList<String> values){
		if(row>0){
			data.put(row, values);
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}
	
	public SparseArray<ArrayList<String>> getData(){
		return data;
	}
	
	public void deleteLine(int row){
		if(row>0){
			//wq:分清楚key和index
			data.delete(data.keyAt(row));

			row_num = data.size();
			if (row_num == 1) {
				INDEX_WIDTH = 0;
			}
			selectedRow=-1;
			resize();
		}
	}

	private void resize() {
		headerRect.set(INDEX_WIDTH, 0, screenWidth, CELL_HEIGHT);
		leftRect.set(0, CELL_HEIGHT, INDEX_WIDTH, screenHeight);
		dataRect.set(INDEX_WIDTH, CELL_HEIGHT, screenWidth, screenHeight);

		if (widthList.get(column_num - 1) <= screenWidth - INDEX_WIDTH) {
			maxOffsetX = 0;
		} else {
			maxOffsetX = widthList.get(column_num - 1) + INDEX_WIDTH
					- screenWidth;
		}

		if (row_num * CELL_HEIGHT <= screenHeight) {
			maxOffsetY = 0;
		} else {
			maxOffsetY = row_num * CELL_HEIGHT - screenHeight;
		}

		hRate = (float) (screenWidth - INDEX_WIDTH)
				/ widthList.get(column_num - 1);
		vRate = (float) (screenHeight - CELL_HEIGHT)
				/ ((row_num - 1) * CELL_HEIGHT);

		hScrollbarLength = hRate * (screenWidth - INDEX_WIDTH);
		vScrollbarLength = vRate * (screenHeight - CELL_HEIGHT);
		
		allDataHeight=row_num*CELL_HEIGHT;
		
		//宽度不够时最后一列补全
		if(widthList.get(column_num-1)<screenWidth-INDEX_WIDTH){
			widthList.set(column_num-1, screenWidth-INDEX_WIDTH);
		}
		ViewCompat.postInvalidateOnAnimation(this);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		screenWidth = w;
		screenHeight = h;
		if (data != null) {
			resize();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (column_num == 0) {
			return;
		}

		drawScrollBar(canvas);

		int minColumn = 0;
		for (int i = 0; i < column_num; i++) {
			if (-offsetX < widthList.get(i)) {
				minColumn = i;
				break;
			}
		}

		int maxColumn = column_num;
		for (int i = 0; i < column_num; i++) {
			if (-offsetX - INDEX_WIDTH + screenWidth <= widthList.get(i)) {
				maxColumn = i + 1;
				break;
			}
		}

		int minRow = (int) (-offsetY / CELL_HEIGHT);
		int maxRow = (int) Math.ceil((-offsetY + screenHeight - CELL_HEIGHT)
				/ CELL_HEIGHT);
		if (minRow > row_num - 1) {
			minRow = row_num - 1;
		}
		if (maxRow > row_num - 1) {
			maxRow = row_num - 1;
		}

		drawSelected(canvas);

		canvas.save();
		// wq:use Region.Op.REPLACE have bug
		canvas.clipRect(headerRect);
		for (int i = minColumn; i < maxColumn; i++) {
			final float left = INDEX_WIDTH
					+ (i == 0 ? 0 : widthList.get(i - 1)) + offsetX;
			int cellWidth = widthList.get(i)
					- (i == 0 ? 0 : widthList.get(i - 1));
			canvas.drawRect(left, 0, left + cellWidth, CELL_HEIGHT, headerPaint);
			canvas.drawRect(left, 0, left + cellWidth, CELL_HEIGHT - 1,
					borderPaint);

			String title = data.valueAt(0).get(i);
			canvas.drawText(title, left + textPadding, CELL_HEIGHT / 2 -((textPaint.descent()+textPaint.ascent())/2), textPaint);
		}
		canvas.restore();

		canvas.save();
		canvas.clipRect(leftRect);
		for (int i = minRow; i < maxRow; i++) {
			final float top = CELL_HEIGHT + CELL_HEIGHT * i + offsetY;
			canvas.drawRect(0, top, INDEX_WIDTH, top + CELL_HEIGHT, headerPaint);
			canvas.drawRect(0, top, INDEX_WIDTH - 1, top + CELL_HEIGHT,
					borderPaint);

			String text = i + 1 + "";
			canvas.drawText(text, (INDEX_WIDTH - textPaint.measureText(text)) / 2,
					top + CELL_HEIGHT / 2 -((textPaint.descent()+textPaint.ascent())/2), textPaint);
		}
		canvas.restore();

		canvas.save();
		canvas.clipRect(dataRect);
		for (int row = minRow; row < maxRow; row++) {
			for (int column = minColumn; column < maxColumn; column++) {
				final float left = INDEX_WIDTH
						+ (column == 0 ? 0 : widthList.get(column - 1))
						+ offsetX;
				final float bottom = 2 * CELL_HEIGHT + CELL_HEIGHT * row
						+ offsetY;

				int cellWidth = widthList.get(column)
						- (column == 0 ? 0 : widthList.get(column - 1));
				if (data.valueAt(row + 1) != null
						&& data.valueAt(row + 1).get(column) != null) {
					String text = data.valueAt(row + 1).get(column);

					//wq:If the length is too long,getTextBounds/breakText will cost too much time.
					if (text.length() > 500) {
						canvas.drawText("\"(:(:(:too long\"",
								textPadding + left, bottom - textPadding,
								textPaint);
					} else {
						final int num = textPaint.breakText(text, true,
								cellWidth - textPadding * 2, null);
						canvas.drawText(text, 0, num, textPadding + left,
								bottom-CELL_HEIGHT / 2 -((textPaint.descent()+textPaint.ascent())/2),
								textPaint);
					}
				}

				canvas.drawLine(left + cellWidth, bottom - cellWidth, left
						+ cellWidth, bottom, seperatorPaint);
				canvas.drawLine(left, bottom, left + cellWidth, bottom,
						seperatorPaint);
			}
		}
		canvas.restore();

	}

	private void drawScrollBar(Canvas canvas) {
		if (hRate < 1) {
			final float wScrollbarLeft = -offsetX * hRate + INDEX_WIDTH;
			if(allDataHeight>screenHeight-scrollbarSize){
				canvas.drawRect(wScrollbarLeft, screenHeight - scrollbarSize,
						wScrollbarLeft + hScrollbarLength, screenHeight, selectedPaint);
			}else {
				canvas.drawRect(wScrollbarLeft, allDataHeight,
						wScrollbarLeft + hScrollbarLength, allDataHeight+scrollbarSize, selectedPaint);
			}
		}

		if (vRate < 1) {
			final float hScrollbarTop = -offsetY * vRate + CELL_HEIGHT;
			canvas.drawRect(screenWidth - scrollbarSize, hScrollbarTop,
					screenWidth, hScrollbarTop + vScrollbarLength, selectedPaint);
		}
	}
	
	private void drawSelected(Canvas canvas) {
		final float selectedTop = CELL_HEIGHT * (selectedRow + 1) + offsetY;
		if (selectedRow != -1 && selectedTop > 0) {
			canvas.drawRect(0, Math.max(selectedTop, CELL_HEIGHT),
					hRate < 1 ? screenWidth : widthList.get(column_num - 1)
							+ INDEX_WIDTH, selectedTop + CELL_HEIGHT,
					selectedPaint);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return mGestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
	}


	////////////////////////////////////
	private final GestureDetector.SimpleOnGestureListener mGestureListener
    = new GestureDetector.SimpleOnGestureListener() {
		@Override
		public boolean onDown(MotionEvent e) {
			mScroller.forceFinished(true);
			ViewCompat.postInvalidateOnAnimation(DataGrid.this);
			return true;
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
				float distanceY) {
			offsetX -= distanceX;
			offsetY -= distanceY;

			if (offsetX > 0) {
				offsetX = 0;
			} else if (offsetX < -maxOffsetX) {
				offsetX = -maxOffsetX;
			}

			if (offsetY > 0) {
				offsetY = 0;
			} else if (offsetY < -maxOffsetY) {
				offsetY = -maxOffsetY;
			}
			
			ViewCompat.postInvalidateOnAnimation(DataGrid.this);
			return true;
		}

		@Override
		public boolean onSingleTapUp(MotionEvent e) {
			selectedRowInvalidate(e.getY());
			ViewCompat.postInvalidateOnAnimation(DataGrid.this);
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			boolean inArea = selectedRowInvalidate(e.getY());
			if (inArea && onLongPressListener != null && selectedRow != -1) {
				onLongPressListener.onLongPress(selectedRow + 1, 
//						data.valueAt(0),
						data.valueAt(selectedRow + 1));
			}
		}
		
		@Override
		public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
				float velocityY) {
			mScroller.forceFinished(true);
			mScroller.fling((int) offsetX, (int) offsetY, (int) velocityX,
					(int) velocityY, -(int) maxOffsetX, 0, -(int) maxOffsetY, 0);
			
			//wq:缺少这个时，滑动不流畅，连续fling会间断
			ViewCompat.postInvalidateOnAnimation(DataGrid.this);
			return true;
		}
		
		/**
		 * @return whether the point belongs to the selectable rows.
		 */
		private boolean selectedRowInvalidate(float y) {
			if (y < CELL_HEIGHT) {
                if (selectedRow >= 0) {
                    onSelectChangedListener.onSelectChanged(0, null);
                    selectedRow=-1;
                    ViewCompat.postInvalidateOnAnimation(DataGrid.this);
                }
                return false;
            }

			boolean isValid;
			if (y > CELL_HEIGHT * row_num) {
				selectedRow = -1;
				isValid = false;
			} else {
				selectedRow = (int) ((y - CELL_HEIGHT - offsetY) / CELL_HEIGHT);
				isValid = true;
			}
			
			if(onSelectChangedListener!=null){
				if(isValid){
					onSelectChangedListener.onSelectChanged(selectedRow+1, data.valueAt(selectedRow + 1));
				}else {
					onSelectChangedListener.onSelectChanged(0, null);
				}
			}
			ViewCompat.postInvalidateOnAnimation(DataGrid.this);
			return isValid;
		}
	};
	////////////////////////////////////
	
	
	
	
	@Override
	public void computeScroll() {
		super.computeScroll();
		if (mScroller.computeScrollOffset()) {
			offsetX = mScroller.getCurrX();
			offsetY = mScroller.getCurrY();

			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	private OnLongPressListener onLongPressListener;
	public void setOnLongPressListener(OnLongPressListener listener) {
		onLongPressListener = listener;
	}
	public interface OnLongPressListener {
		void onLongPress(int selectedRow, ArrayList<String> value);
	}
	
	private OnSelectChangedListener onSelectChangedListener;
	public void setOnSelectChangedListener(OnSelectChangedListener listener){
		onSelectChangedListener=listener;
	}
	public interface OnSelectChangedListener{
		void onSelectChanged(int selectedRow, ArrayList<String> value);
	}
}