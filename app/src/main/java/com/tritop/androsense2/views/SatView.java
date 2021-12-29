/*
    Copyright (C) 2013-2014 Christian Schneider
    christian.d.schneider@googlemail.com
    
    This file is part of Androsens 2.

    Androsens 2 is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Androsens 2 is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Androsens 2.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.tritop.androsense2.views;

import java.util.Iterator;
import java.util.Vector;

import com.tritop.androsense2.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.location.GpsSatellite;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


public class SatView extends View {
	
	/*this is smooth value for the low pass filter*/
	private float mLowpassSmooth = 0.10f;
	
	/*factor defines the size of grid*/
	private float SFACTOR =0.95f;
	
	/*used for canvas rotation*/
	private float azRotation=0;
	
	/*Color of the satellite position grid*/
	private int mGridColor = Color.GRAY;
	
	/*Color of the gps satellites dots*/
	private int mGpsSatColor = getResources().getColor(R.color.skyblue);
	
	/*Color of the circle around gps satellites indicating fix*/
	private int mGpsFixColor = getResources().getColor(R.color.fixcolor);
	
	/*Color of the glonass satellites dots*/
	private int mGlonassSatColor = getResources().getColor(R.color.grassgreen);
	
	/*Color of the circle around glonass satellites indicating fix*/
	private int mGlonassFixColor = getResources().getColor(R.color.fixcolor);
	
	private int xCenter,yCenter,xSize,ySize;
	private Vector<GpsSatellite> mGpsSats = new Vector<GpsSatellite>();
	
	public SatView(Context context) {
		super(context);
		invalidate();
	}

	
	public SatView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		invalidate();
	}



	
	public SatView(Context context, AttributeSet attrs) {
		super(context, attrs);
		invalidate();
	}



	public void setGpsSatColor(int mGpsSatColor) {
		this.mGpsSatColor = mGpsSatColor;
	}



	public void setGlonassSatColor(int mGlonassSatColor) {
		this.mGlonassSatColor = mGlonassSatColor;
	}



	public void setGpsFixColor(int mGpsFixColor) {
		this.mGpsFixColor = mGpsFixColor;
	}



	public void setGlonassFixColor(int mGlonassFixColor) {
		this.mGlonassFixColor = mGlonassFixColor;
	}



	public void setSatellites(Iterable<GpsSatellite> sats){
		this.mGpsSats.clear();
		Iterator<GpsSatellite> iter = sats.iterator();
		while(iter.hasNext()){
			this.mGpsSats.add(iter.next());
		}
	}
	
	
	public void setAzimutRotation(float rot){
		this.azRotation = lowPass(this.azRotation, rot,mLowpassSmooth);
		//this.azRotation = rot;
	}
	
	
	public void setLowPassSmoothness(float smoothness){
		this.mLowpassSmooth = smoothness;
	}
	
	/**
	 * low pass filter for rotation values
	 */
	private float lowPass(float oldValue, float newValue,float alpha){
		return oldValue+ (alpha* (newValue-oldValue) );
	}
	
	
	/**
	 * Draws the grid on our canvas
	 * @param canvas
	 */
	private void drawGrid(Canvas canvas){
		int minSide = (this.xSize < this.ySize)?this.xSize:this.ySize;
		int gridSize = (this.xSize + this.ySize)/ 400 +1;
		Rect bounds = new Rect();
		Paint gridPaint = new Paint();
		gridPaint.setColor(mGridColor);
		gridPaint.setStyle(Style.STROKE);
		gridPaint.setAntiAlias(true);
		gridPaint.setStrokeWidth(gridSize);
		gridPaint.getTextBounds("0o", 0, 1, bounds);
		
		/*draw grid circles*/
		canvas.drawCircle(this.xCenter, this.yCenter, (minSide/2)*SFACTOR, gridPaint);
		canvas.drawCircle(this.xCenter, this.yCenter, (minSide/2)*(SFACTOR/2), gridPaint);
		
		/*draw grid crosshair lines*/
		canvas.drawLine(this.xCenter-(minSide/2)*SFACTOR, this.yCenter, this.xCenter+(minSide/2)*SFACTOR, this.yCenter, gridPaint);
		canvas.drawLine(this.xCenter, this.yCenter-(minSide/2)*SFACTOR, this.xCenter, this.yCenter+(minSide/2)*SFACTOR, gridPaint);
		
		/*set color and size for the grid text annotaion*/
		gridPaint.setColor(Color.BLACK);
		gridPaint.setStrokeWidth(gridSize/2);
		gridPaint.setTextSize(gridSize*6);
		
		/*draw grid top text annotation*/
		canvas.drawText("0", this.xCenter-bounds.width(), (this.yCenter+(minSide/2)*SFACTOR)+bounds.height(), gridPaint);
		canvas.drawText("45", this.xCenter-bounds.width(), (this.yCenter+(minSide/2)*(SFACTOR/2))+bounds.height(), gridPaint);
		
		/*draw grid bottom text annotation*/
		canvas.drawText("0", this.xCenter-bounds.width(), (this.yCenter-(minSide/2)*SFACTOR)+bounds.height(), gridPaint);
		canvas.drawText("45", this.xCenter-bounds.width(), (this.yCenter-(minSide/2)*(SFACTOR/2))+bounds.height(), gridPaint);
	}
	
	/**
	 * Draws the satellite dots on the canvas
	 * @param canvas
	 */
	private void drawSatellites(Canvas canvas){
		Matrix rotationMatrix = new Matrix();
		int minSide = (this.xSize < this.ySize)?this.xSize:this.ySize;
		int col;
		Paint satPaint = new Paint();
		satPaint.setStyle(Style.STROKE);
		satPaint.setAntiAlias(true);
		satPaint.setStrokeWidth(7);
		
		/*iterate over all satellites we found*/
		for(GpsSatellite sat: this.mGpsSats){
			float[] satPoint = new float[2];
			satPoint[0] = this.xCenter;
			
			/*process the hight of the point indicating the satellite elevation*/
			satPoint[1] = this.yCenter- (((minSide/2)*SFACTOR)*(((90-sat.getElevation())/90)));
			
			/*then use the rotation matrix to map the point to the position*/
			rotationMatrix.setRotate(sat.getAzimuth(), this.xCenter, this.yCenter);
			/*where it should be on a device that is not pointing north*/
			rotationMatrix.mapPoints(satPoint);
			
			/*use a different color for gps and glonass satellite points*/
			col = (sat.getPrn()<=32)?this.mGpsSatColor:this.mGlonassSatColor;
			satPaint.setColor(col);
			canvas.drawPoint(satPoint[0], satPoint[1], satPaint);
			
			/*if this satellite is used in fix, we will draw a circle around it*/
			if(sat.usedInFix()){
				col = (sat.getPrn()<=32)?this.mGpsFixColor:this.mGlonassFixColor;
				satPaint.setColor(col);
				canvas.drawCircle(satPoint[0], satPoint[1], 10, satPaint);
			}
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		
		/*first determine canvas size*/
		this.xSize = this.getMeasuredWidth();
		this.ySize = this.getMeasuredHeight();
		this.xCenter= this.getMeasuredWidth()/2;
		this.yCenter= this.getMeasuredHeight()/2;
		
		/*rotate canvas depending on device orientation*/
		canvas.rotate(this.azRotation, this.xCenter, this.yCenter);
		
		/*draw the grid on our canvas*/
		drawGrid(canvas);
		
		/*draw the satellites on our canvas*/
		drawSatellites(canvas);
	}

}
