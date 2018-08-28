/**
 * 
 */
package com.sinosoft.controller;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import com.sinosoft.dto.PictureDTO;

/**
 * @author Nov。
 * @date 2018年8月25日 
 * 
1.加载原图像并进行处理到PictureDTO

2.拿到顶点时绘制一张新的图片，绘制新图片(除轮廓外全部置成黑色，轮廓纯白)，方便旋转之后寻找角点  

3.对原图像进行旋转  

4.对背景为黑，轮廓填充为白色的新图进行旋转

5.对步骤4的图片进行轮廓查找,拿到填充图的四角顶点，，对步骤3的图片进行裁剪
这样做是因为有可能两次寻找角点不一致，导致图片裁剪失败
 */

public class PictureController {
	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Mat src = Imgcodecs.imread("./images/notok.jpg");
		Mat result = src.clone();
		PictureDTO corners = getCorners(src);
		Point[] points = corners.getPoints();
		double imgWidth = corners.getImgWidth();
		double imgHeight = corners.getImgHeight();
		// 提取背景图像
		List<MatOfPoint> matOfPoints = new ArrayList<>();
		MatOfPoint matOfPoint = new MatOfPoint(points);
		matOfPoints.add(matOfPoint);
		Mat mat = new Mat();
		mat = result.clone();
		mat.setTo(new Scalar(0, 0, 0));
		// 利用一张新的图片，背景为黑，将感兴趣的区域填充为白色
		Imgproc.fillPoly(mat, matOfPoints, new Scalar(255, 255, 255));
		Imgcodecs.imwrite("./finally/allblack.jpg", mat);
		Mat quad = Mat.zeros((int) imgHeight * 2, (int) imgWidth * 2, CvType.CV_8UC3);
		MatOfPoint2f cornerMat = new MatOfPoint2f(points);
		MatOfPoint2f quadMat = new MatOfPoint2f(new Point(imgWidth * 0.4, imgHeight * 1.6),
				new Point(imgWidth * 0.4, imgHeight * 0.4), new Point(imgWidth * 1.6, imgHeight * 0.4),
				new Point(imgWidth * 1.6, imgHeight * 1.6));

		// 提取图像-->旋转原图
		Mat transmtx = Imgproc.getPerspectiveTransform(cornerMat, quadMat);
		Imgproc.warpPerspective(result, quad, transmtx, quad.size());
		Imgcodecs.imwrite("./finally/src2D.jpg", quad);
		// 提取图像-->旋转背景图
		Mat allblack2D = new Mat();
		Imgproc.warpPerspective(mat, allblack2D, transmtx, quad.size());
		Imgcodecs.imwrite("./finally/allblack2D.jpg", allblack2D);

		// 对纯色图片allblack2D进行角点检测，获取左上角个顶点
		PictureDTO pictureDTO = getCorners(allblack2D);
		Point[] points2 = pictureDTO.getPoints();
		Point sortPoint = sortPoint(points2);
		Rect roi = new Rect((int) sortPoint.x, (int) sortPoint.y, (int) pictureDTO.getImgWidth(),
				(int) pictureDTO.getImgHeight());

		// 指定图像格式大小
		Mat rec = new Mat(quad, roi);
		Imgcodecs.imwrite("./finally/finally.jpg", rec);
	}

	// 根据三个点计算中间那个点的夹角 pt1 pt0 pt2
	private static double getAngle(Point pt1, Point pt2, Point pt0) {
		double dx1 = pt1.x - pt0.x;
		double dy1 = pt1.y - pt0.y;
		double dx2 = pt2.x - pt0.x;
		double dy2 = pt2.y - pt0.y;
		return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
	}

	// 找到最大的正方形轮廓
	private static int findLargestSquare(List<MatOfPoint> squares) {
		if (squares.size() == 0)
			throw new RuntimeException("未能找到最大正方形轮廓");
		int max_width = 0;
		int max_height = 0;
		int max_square_idx = 0;
		int currentIndex = 0;
		for (MatOfPoint square : squares) {
			Rect rectangle = Imgproc.boundingRect(square);
			if (rectangle.width >= max_width && rectangle.height >= max_height) {
				max_width = rectangle.width;
				max_height = rectangle.height;
				max_square_idx = currentIndex;
			}
			currentIndex++;
		}
		return max_square_idx;
	}

	// 点到点的距离
	private static double getSpacePointToPoint(Point p1, Point p2) {
		double a = p1.x - p2.x;
		double b = p1.y - p2.y;
		return Math.sqrt(a * a + b * b);
	}

	// 两直线的交点
	private static Point computeIntersect(double[] a, double[] b) {
		if (a.length != 4 || b.length != 4)
			throw new ClassFormatError();
		double x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3], x3 = b[0], y3 = b[1], x4 = b[2], y4 = b[3];
		double d = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
		if (d != 0) {
			Point pt = new Point();
			pt.x = ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
			pt.y = ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
			return pt;
		} else
			return new Point(-1, -1);
	}

	// 对多个点按顺时针排序
	private static void sortCorners(List<Point> corners) {
		if (corners.size() == 0)
			return;
		Point p1 = corners.get(0);
		int index = 0;
		for (int i = 1; i < corners.size(); i++) {
			Point point = corners.get(i);
			if (p1.x > point.x) {
				p1 = point;
				index = i;
			}
		}
		corners.set(index, corners.get(0));
		corners.set(0, p1);
		Point lp = corners.get(0);
		for (int i = 1; i < corners.size(); i++) {
			for (int j = i + 1; j < corners.size(); j++) {
				Point point1 = corners.get(i);
				Point point2 = corners.get(j);
				if ((point1.y - lp.y * 1.0) / (point1.x - lp.x) > (point2.y - lp.y * 1.0) / (point2.x - lp.x)) {
					Point temp = point1.clone();
					corners.set(i, corners.get(j));
					corners.set(j, temp);
				}
			}
		}
	}

	private static PictureDTO getCorners(Mat src) {
		//灰度处理
		Imgproc.cvtColor(src, src, Imgproc.COLOR_BGR2GRAY);
		// 高斯滤波，降噪
		Imgproc.GaussianBlur(src, src, new Size(3, 3), 2, 2);
		// Canny边缘检测 阔阈值的大小直接影响轮廓清晰度
		Imgproc.Canny(src, src, 30, 90, 3, false);
		// 膨胀，连接边缘
		Imgproc.dilate(src, src, new Mat(), new Point(-1, -1), 3, 1, new Scalar(1));
		List<MatOfPoint> contours = new ArrayList<>();
		Mat hierarchy = new Mat();
		//寻找轮廓
		Imgproc.findContours(src, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		List<MatOfPoint> squares = new ArrayList<>();
		List<MatOfPoint> hulls = new ArrayList<>();
		MatOfInt hull = new MatOfInt();
		MatOfPoint2f approx = new MatOfPoint2f();
		approx.convertTo(approx, CvType.CV_32F);
		for (MatOfPoint contour : contours) {
			// 边框的凸包
			Imgproc.convexHull(contour, hull);
			// 用凸包计算出新的轮廓点
			Point[] contourPoints = contour.toArray();
			int[] indices = hull.toArray();
			List<Point> newPoints = new ArrayList<>();
			for (int index : indices) {
				newPoints.add(contourPoints[index]);
			}
			MatOfPoint2f contourHull = new MatOfPoint2f();
			contourHull.fromList(newPoints);

			// 多边形拟合凸包边框(此时的拟合的精度较低)
			Imgproc.approxPolyDP(contourHull, approx, Imgproc.arcLength(contourHull, true) * 0.02, true);

			// 筛选出面积大于某一阈值的，且四边形的各个角度都接近直角的凸四边形
			MatOfPoint approxf1 = new MatOfPoint();
			approx.convertTo(approxf1, CvType.CV_32S);
			if (approx.rows() == 4 && Math.abs(Imgproc.contourArea(approx)) > 40000
					&& Imgproc.isContourConvex(approxf1)) {
				double maxCosine = 0;
				for (int j = 2; j < 5; j++) {
					double cosine = Math.abs(
							getAngle(approxf1.toArray()[j % 4], approxf1.toArray()[j - 2], approxf1.toArray()[j - 1]));
					maxCosine = Math.max(maxCosine, cosine);
				}
				if (maxCosine < 0.3) {
					MatOfPoint tmp = new MatOfPoint();
					contourHull.convertTo(tmp, CvType.CV_32S);
					squares.add(approxf1);
					hulls.add(tmp);
				}
			}
		}
		// 找出外接矩形最大的四边形
		int index = findLargestSquare(squares);
		MatOfPoint largest_square = squares.get(index);
		if (largest_square.rows() == 0 || largest_square.cols() == 0) {
			throw new RuntimeException("发生未知错误");
		}
		// 找到这个最大的四边形对应的凸边框，再次进行多边形拟合，此次精度较高，拟合的结果可能是大于4条边的多边形
		MatOfPoint contourHull = hulls.get(index);
		MatOfPoint2f tmp = new MatOfPoint2f();
		contourHull.convertTo(tmp, CvType.CV_32F);
		Imgproc.approxPolyDP(tmp, approx, 3, true);
		List<Point> newPointList = new ArrayList<>();
		double maxL = Imgproc.arcLength(approx, true) * 0.02;
		// 找到高精度拟合时得到的顶点中 距离小于低精度拟合得到的四个顶点maxL的顶点，排除部分顶点的干扰
		for (Point p : approx.toArray()) {
			if (!(getSpacePointToPoint(p, largest_square.toList().get(0)) > maxL
					&& getSpacePointToPoint(p, largest_square.toList().get(1)) > maxL
					&& getSpacePointToPoint(p, largest_square.toList().get(2)) > maxL
					&& getSpacePointToPoint(p, largest_square.toList().get(3)) > maxL)) {
				newPointList.add(p);
			}
		}
		// 找到剩余顶点连线中，边长大于 2 * maxL的四条边作为四边形物体的四条边
		List<double[]> lines = new ArrayList<>();
		for (int i = 0; i < newPointList.size(); i++) {
			Point p1 = newPointList.get(i);
			Point p2 = newPointList.get((i + 1) % newPointList.size());
			if (getSpacePointToPoint(p1, p2) > 2 * maxL) {
				lines.add(new double[] { p1.x, p1.y, p2.x, p2.y });
			}
		}
		// 计算出这四条边中 相邻两条边的交点，即物体的四个顶点
		List<Point> corners = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			Point corner = computeIntersect(lines.get(i), lines.get((i + 1) % lines.size()));
			corners.add(corner);
		}
		// 对顶点顺时针排序
		sortCorners(corners);
		// 计算目标图像的尺寸
		Point p0 = corners.get(0);
		Point p1 = corners.get(1);
		Point p2 = corners.get(2);
		Point p3 = corners.get(3);

		double space0 = getSpacePointToPoint(p0, p1);
		double space1 = getSpacePointToPoint(p1, p2);
		double space2 = getSpacePointToPoint(p2, p3);
		double space3 = getSpacePointToPoint(p3, p0);

		double imgWidth = space1 > space3 ? space1 : space3;
		double imgHeight = space0 > space2 ? space0 : space2;
		// 如果提取出的图片宽小于高，则旋转90度
		if (imgWidth < imgHeight) {
			double temp = imgWidth;
			imgWidth = imgHeight;
			imgHeight = temp;
			Point tempPoint = p0.clone();
			p0 = p1.clone();
			p1 = p2.clone();
			p2 = p3.clone();
			p3 = tempPoint.clone();
		}
		// 封装point
		Point[] points = new Point[4];
		points[0] = p0;
		points[1] = p1;
		points[2] = p2;
		points[3] = p3;
		PictureDTO pictureDTO=new PictureDTO(points,imgWidth,imgHeight);
		return pictureDTO;
	}
	//对摆正图形进行建材剪裁，得到左上角顶点坐标
	private static Point sortPoint(Point[] points){
		double minX=points[0].x;
		double minY=points[0].y;
		
		for(int i=1;i<points.length;i++){
			if(minX>points[i].x){
				minX=points[i].x;
			}
			if(minY>points[i].y){
				minY=points[i].y;
			}
		}
		return new Point(minX,minY);
	}
}
