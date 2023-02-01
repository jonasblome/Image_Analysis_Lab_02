/**
 * @author Nico Hezel
 * modified by K. Jung, 28.10.2016
 */
package de.htw.ba.ue02.controller;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class HoughTransformationController extends HoughTransformationBase {
	
	protected static enum Methods {
		Empty, Accumulator, Maximum, Line
	};

	@Override
	public void runMethod(Methods currentMethod, int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight, float sliderValue) throws Exception {
		switch (currentMethod) {
			case Accumulator:
				showAcc(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
				break;
			case Maximum:
				showMax(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, sliderValue);
				break;
			case Line:
				showLines(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, sliderValue);
				break;
			case Empty:
			default:
				empty(dstPixels, dstWidth, dstHeight);
				break;
		}
	}

	private void empty(int[] dstPixels, int dstWidth, int dstHeight) {
		// all pixels black
		Arrays.fill(dstPixels, 0xff000000);
	}
	
	private void showAcc(int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight) {
		empty(dstPixels, dstWidth, dstHeight);
		int radiusDivisions = 500;
		int angleDivisions = 360;
		int[] accu = new int[radiusDivisions * angleDivisions];
		double maxRadius = Math.sqrt(Math.pow((srcHeight), 2) + Math.pow((srcWidth), 2)) / 2;

		// Loop over all pixels in source image
		for (int y = 0; y < srcHeight; y++) {
			for (int x = 0; x < srcWidth; x++) {
				int yShift = - (y - (srcHeight / 2));
				int xShift = x - (srcWidth / 2);
				int pos = y * srcWidth + x;

				if((0xFF & srcPixels[pos]) == 255) {
					for (int a = 0; a < angleDivisions; a++) {
						double angle = Math.toRadians(a * 1.0/ 2);
						double radiusShift = (xShift * Math.cos(angle) + yShift * Math.sin(angle));
						int radius = (int) (((radiusShift / maxRadius) * (radiusDivisions * 1.0 / 2)) + (radiusDivisions * 1.0 / 2));
						int accuPos = radius * angleDivisions + a;

						accu[accuPos]++;
					}
				}
			}
		}

		// Normalize accumulator array for display
		int max = Arrays.stream(accu).max().getAsInt();
		for(int i = 0; i < accu.length; i++) {
			accu[i] = (int) (accu[i] * 1.0 / max * 255);
			dstPixels[i] = 0xFF000000 | accu[i] << 16 | accu[i] << 8 | accu[i];
		}
	}
	
	private void showMax(int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight, float sliderValue) {
		empty(dstPixels, dstWidth, dstHeight);
		showAcc(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
		int[] accuMax = new int[dstWidth * dstHeight];
		Arrays.fill(accuMax, -1);
		int filterSize = 10;

		// Loop over all accumulator values
		for (int r = 0; r < dstHeight; r++) {
			for (int a = 0; a < dstWidth; a++) {
				int pos = r * dstWidth + a;

				int accuVal = 0xFF & dstPixels[pos];

				// Check if value is above threshold
				if(accuVal > sliderValue * 255) {
					// Loop over all values in filter
					for (int y = -filterSize; y < filterSize + 1; y++) {
						for (int x = -filterSize; x < filterSize + 1; x++) {
							int comparatorValue;

							// Handle edge cases
							if(r + y > dstHeight || r + y < 0 || a + x > dstWidth || a + x < 0) {
								comparatorValue = 0;
							}
							else {
								int posWithFilter =  (r + y) * dstWidth + a + x;
								comparatorValue = 0xFF & dstPixels[posWithFilter];
							}

							// Check if value at filter position is above current accumulator value
							if(comparatorValue > accuVal) {
								accuMax[pos] = 0;
							}
						}
					}
					// If no higher value in area has been found, store current accumulator value
					if(accuMax[pos] == -1) {
						accuMax[pos] = accuVal;
					}
				}
				else {
					accuMax[pos] = 0;
				}
			}
		}

		// Normalize accumulator array for display
		int max = Arrays.stream(accuMax).max().getAsInt();
		for(int i = 0; i < accuMax.length; i++) {
			accuMax[i] = (int) (accuMax[i] * 1.0 / max * 255);
			dstPixels[i] = 0xFF000000 | accuMax[i] << 16 | accuMax[i] << 8 | accuMax[i];
		}
	}
	
	private void showLines(int[] srcPixels, int srcWidth, int srcHeight, int[] dstPixels, int dstWidth, int dstHeight, float sliderValue) {
		empty(dstPixels, dstWidth, dstHeight);
		showAcc(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight);
		showMax(srcPixels, srcWidth, srcHeight, dstPixels, dstWidth, dstHeight, sliderValue);
		double maxRadius = Math.sqrt(Math.pow((srcHeight), 2) + Math.pow((srcWidth), 2)) / 2;
		BufferedImage bufferedImage = new BufferedImage(srcWidth, srcHeight, BufferedImage.TYPE_INT_ARGB);
		bufferedImage.setRGB(0, 0, srcWidth, srcHeight, srcPixels, 0, srcWidth);
		Graphics2D g2d = bufferedImage.createGraphics();
		g2d.setColor(Color.RED);

		// Looping over all pixels to draw line for each max value
		for (int y = 0; y < dstHeight; y++) {
			for (int x = 0; x < dstWidth; x++) {
				int pos = y * dstWidth + x;

				if((0xFF & dstPixels[pos]) > 0) {
					double angle = Math.toRadians(x * 1.0 / 2);
					double radius = (y - dstHeight * 1.0 / 2) / (dstHeight * 1.0 / 2) * maxRadius;
					int y1 = (int) ((radius - (-srcWidth / 2) * Math.cos(angle)) / Math.sin(angle));
					int y2 = (int) ((radius - (srcWidth / 2) * Math.cos(angle)) / Math.sin(angle));
					y1 = - y1 + srcHeight / 2;
					y2 = - y2 + srcHeight / 2;
					g2d.drawLine(0, y1, srcWidth, y2);
				}
			}
		}
		g2d.dispose();
		bufferedImage.getRGB(0, 0, srcWidth, srcHeight, srcPixels, 0, srcWidth);
	}

}
