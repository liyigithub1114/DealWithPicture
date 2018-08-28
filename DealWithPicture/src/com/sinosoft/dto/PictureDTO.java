/**
 * 
 */
package com.sinosoft.dto;

import java.io.Serializable;
import java.util.Arrays;

import org.opencv.core.Point;

/**
 * 
 * @author Nov。
 * @date 2018年8月25日
 * 用于封装找到图片时的轮廓属性 
 * 包括四个顶点:points   
 * 图片长度:imgHeight   图片宽度:imgWidth
 */
public class PictureDTO implements Serializable{
	private Point[] points;
	private double imgWidth;
	private double imgHeight;
	/**
	 * @param points
	 * @param imgWidth
	 * @param imgHeight
	 */
	public PictureDTO(Point[] points, double imgWidth, double imgHeight) {
		super();
		this.points = points;
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;
	}
	/**
	 * 
	 */
	public PictureDTO() {
		super();
	}
	/**
	 * @return the points
	 */
	public Point[] getPoints() {
		return points;
	}
	/**
	 * @param points the points to set
	 */
	public void setPoints(Point[] points) {
		this.points = points;
	}
	/**
	 * @return the imgWidth
	 */
	public double getImgWidth() {
		return imgWidth;
	}
	/**
	 * @param imgWidth the imgWidth to set
	 */
	public void setImgWidth(double imgWidth) {
		this.imgWidth = imgWidth;
	}
	/**
	 * @return the imgHeight
	 */
	public double getImgHeight() {
		return imgHeight;
	}
	/**
	 * @param imgHeight the imgHeight to set
	 */
	public void setImgHeight(double imgHeight) {
		this.imgHeight = imgHeight;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "PictureDTO [points=" + Arrays.toString(points) + ", imgWidth=" + imgWidth + ", imgHeight=" + imgHeight
				+ "]";
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(imgHeight);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(imgWidth);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + Arrays.hashCode(points);
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PictureDTO other = (PictureDTO) obj;
		if (Double.doubleToLongBits(imgHeight) != Double.doubleToLongBits(other.imgHeight))
			return false;
		if (Double.doubleToLongBits(imgWidth) != Double.doubleToLongBits(other.imgWidth))
			return false;
		if (!Arrays.equals(points, other.points))
			return false;
		return true;
	}
	
	
}
