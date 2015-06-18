package org.safs.android.engine.processor;

import java.util.HashMap;

import org.safs.android.engine.DSAFSTestRunner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.Shape;
import android.view.View;
import android.view.WindowManager.BadTokenException;
import android.widget.PopupWindow;

/**
 * Purpose: Help to highlight a view.
 * 
 * @author Lei Wang, SAS Institute, Inc
 *
 * (Lei Wang) MAR 27, 2013  Use PopupWindow to show the highlight rectangle.<br>
 */
public class HighLightUtil {
	private DSAFSTestRunner runner = null;
	private View view = null;
	
	private HashMap<Object, HighLightStatus> statusMap = new HashMap<Object, HighLightStatus>();
	private final String TAG = HighLightUtil.class.getSimpleName()+": ";
	
	public HighLightUtil(DSAFSTestRunner runner, View view){
		this.runner = runner;
		setView(view);
	}
	
	public void setView(View view){
		this.view = view;
	}
	
	/**
	 * Try to highlight a view.<br>
	 * First try to highlight by placing a PopupWindow over the view.<br>
	 * If PopupWindow fails to show, we will try to modify the background of the view to highlight.<br>
	 */
	public boolean highLight(){
		boolean success = false;
		HighLightStatus status = null;

		try{
			if(view!=null){
				//Get the view's rectangle on screen
				Rect rect = new Rect();
				view.getDrawingRect(rect);
				debug(TAG +view.getClass().getSimpleName()+view+": rectangle is "+rect.toString()+"; visible="+view.isShown());
				//Show the view on screen, will scroll if needed.
				view.requestRectangleOnScreen(rect);

				//TODO Some items in a ListView or GridView, if they are not shown on screen (off-screen)
				//they are not visible, View.setBackgroundDrawable() will not work, how to highlight them???
				if(view.isShown()){
					
					if(!statusMap.containsKey(view)){
						statusMap.put(view, new HighLightStatus(false, view.getBackground()));
					}
					status = statusMap.get(view);
					
					if(!status.isHightLighted()){
						PopupWindow highlightWidown = null;
						try{
							//First, try to highlight by placing a PopupWindow over the view
							HighlightView highlightView = new HighlightView(runner.getTargetContext(), rect.height(), rect.width());
							highlightWidown = new PopupWindow(highlightView, rect.width(), rect.height());
							//API showAsDropDown() may throw BadTokenException, but HighlightView was added to AUT
							highlightWidown.showAsDropDown(view, 0, -rect.height());
							status.setHighlightWidown(highlightWidown);
							status.hightLightedByPopupWindow = true;
						}catch(BadTokenException e){
							//Release the HighlightView from AUT
							try{highlightWidown.dismiss();}catch(Exception ignore){}
							status.hightLightedByPopupWindow = false;
	
							if(status.getHighLightedBackground()==null){
								status.setHighLightedBackground(createHighLightedBackground(view));
							}
							view.setBackgroundDrawable(status.getHighLightedBackground());
						}
						status.setHightLighted(true);
					}else{
						debug(TAG +"view '"+view.getClass().getSimpleName()+"' is already highlighted.");
					}
				    
					success = true;
				}else{
					debug(TAG +"view '"+view.getClass().getSimpleName()+"' is not shown on screen.");
					success = false;
				}

			}else{
				debug(TAG +"view is null.");
				success = false;
			}
		}catch(Exception e){
			debug(TAG +"met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
			if(status!=null){
				status.setHightLighted(false);
			}
			success = false;
		}
		return success;
	}
	
	public boolean clearHighLight(){
		boolean success = false;
		HighLightStatus status = null;
		
		try{
			if(view!=null && statusMap.containsKey(view)){
				status = statusMap.get(view);
				if(status.isHightLighted()){
					if(status.hightLightedByPopupWindow){
						status.getHighlightWidown().dismiss();
						status.setHighlightWidown(null);
					}else{
						debug(TAG +" highlighted by setting background.");
						view.setBackgroundDrawable(status.getOriginalBackground());
					}
					
					status.setHightLighted(false);			
					success = true;
				}else{
					debug(TAG +"view '"+view.getClass().getSimpleName()+"' is not highlighted. No need to clear.");
				}
			}else{
				debug(TAG +" the view is null or statusMap doesn't contain view "+view);
				success = false;
			}
		}catch(Exception e){
			debug(TAG +"met Exception "+e.getClass().getSimpleName()+":"+e.getMessage());
			success = false;
		}
		return success;
	}
	
	public void debug(String message){
		if(runner!=null){
			runner.debug(message);
		}else{
			System.out.println(message);
		}
	}
	
	/**
	 * Create a LayerDrawable as highlighted background.
	 * This LayerDrawable will contain a {@link ShapeDrawable} created with {@link RectangleShape}.
	 * If the parameter view's background is not null, then LayerDrawable will contain it.
	 * 
	 * @see RectangleShape
	 */
	public Drawable createHighLightedBackground(View view){
		RectangleShape rs = new RectangleShape(view.getWidth(), view.getHeight());
		ShapeDrawable sd = new ShapeDrawable(rs);
		
		Drawable[] layers = {sd};
		LayerDrawable ld = new LayerDrawable(layers);
		if(view.getBackground()!=null){
			Drawable[] layers2 = {sd, view.getBackground()};
			ld = new LayerDrawable(layers2);
		}
		
		return ld;
	}
}

/**
 * This class decide how to draw the highlight of a View.<br>
 * It will be used to create a PopupWindow to show over the View.<br> 
 */
class HighlightView extends View {  
	private int outlineColor = Color.RED;
	private int outlineWidth = 5;
	private int rectangleHeight = 0;
	private int rectangleWidth = 0;
	private Paint paint = null;
	
    public HighlightView(Context context, int rectangleHeight, int rectangleWidth) {  
        super(context);
        this.rectangleHeight = rectangleHeight;
        this.rectangleWidth = rectangleWidth;
        paint = new Paint();
        paint.setColor(outlineColor);
        paint.setStrokeWidth(outlineWidth);
    }
    
    public HighlightView(Context context, int rectangleHeight, int rectangleWidth, int outlineColor, int outlineWidth) {  
    	this(context, rectangleHeight, rectangleWidth);
        this.outlineColor = outlineColor;
        this.outlineWidth = outlineWidth;
    } 
  
    @Override  
    protected void onDraw(Canvas canvas) {  
        super.onDraw(canvas);

		//Draw an rectangle outline
		canvas.drawLine(0, 0, 0, rectangleHeight, paint);
		canvas.drawLine(0, rectangleHeight, rectangleWidth, rectangleHeight, paint);
		canvas.drawLine(rectangleWidth, rectangleHeight, rectangleWidth, 0, paint);
		canvas.drawLine(rectangleWidth, 0, 0, 0, paint);
    }  
}

/**
 * This class decide how to draw the highlight of a View.<br>
 * It will be used to create a Drawable with the view's original background.<br>
 * The Drawable will be set as view's background to show highlight.<br>
 */
class RectangleShape extends Shape {
	private int outlineColor = Color.RED;
	private int outlineWidth = 5;

	public RectangleShape(float width, float height) {
		resize(width, height);
	}

    public RectangleShape(float width, float height, int outlineColor, int outlineWidth) {  
    	this(width, height);
        this.outlineColor = outlineColor;
        this.outlineWidth = outlineWidth;
    }
    
	/**
	 * Modify this method to change the highlight shape.
	 */
	@Override
	public void draw(Canvas canvas, Paint paint) {
		paint.setStrokeWidth(outlineWidth);
		paint.setColor(outlineColor);

		// Draw an rectangle outline around the view
		canvas.drawLine(0, 0, 0, getHeight(), paint);
		canvas.drawLine(0, getHeight(), getWidth(), getHeight(), paint);
		canvas.drawLine(getWidth(), getHeight(), getWidth(), 0, paint);
		canvas.drawLine(getWidth(), 0, 0, 0, paint);

	}
}

/**
 * This class contains the variables to indicate the highlight status of a View.
 */
class HighLightStatus{
	/**
	 * If the view is highlighted or not.
	 */
	boolean hightLighted = false;
	
	/**
	 * originalBackground contains the original BackGround of a View<br>
	 * it should not be modified after it has been assigned.<br>
	 * only {@link #hightLightedByPopupWindow} is false, this field will be used.<br>
	 */
	private Drawable originalBackground = null;
	/**
	 * highLightedBackground contains the highlighted BackGround of a View<br>
	 * only {@link #hightLightedByPopupWindow} is false, this field will be used.<br>
	 */
	private Drawable highLightedBackground = null;

	/**
	 * hightLightedByPopupWindow indicates if the view is highlighted by PopupWindow or by<br>
	 * setting its background.<br>
	 */
	boolean hightLightedByPopupWindow = false;
	/**
	 * highlightWidown will be shown over the view to highlight it.<br>
	 * only {@link #hightLightedByPopupWindow} is true, this field will be used.<br>
	 */
	private PopupWindow highlightWidown = null;
	
	public HighLightStatus(boolean hightLighted, Drawable originalBackground){
		this.hightLighted = hightLighted;
		this.originalBackground = originalBackground;
	}
	
	public HighLightStatus(boolean hightLighted, Drawable originalBackground, Drawable highLightedBackground){
		this(hightLighted, originalBackground);
		this.highLightedBackground = highLightedBackground;
	}

	public boolean isHightLighted() {
		return hightLighted;
	}

	public void setHightLighted(boolean hightLighted) {
		this.hightLighted = hightLighted;
	}

	public Drawable getOriginalBackground() {
		return originalBackground;
	}

	public void setOriginalBackground(Drawable originalBackground) {
		this.originalBackground = originalBackground;
	}

	public Drawable getHighLightedBackground() {
		return highLightedBackground;
	}

	public void setHighLightedBackground(Drawable highLightedBackground) {
		this.highLightedBackground = highLightedBackground;
	}

	public PopupWindow getHighlightWidown() {
		return highlightWidown;
	}

	public void setHighlightWidown(PopupWindow highlightWidown) {
		this.highlightWidown = highlightWidown;
	}
	
}
