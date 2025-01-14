/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.pdf.pdfbox.visible.nativedrawer;

import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.pades.DSSFileFont;
import eu.europa.esig.dss.pades.DSSFont;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pdf.pdfbox.visible.AbstractPdfBoxSignatureDrawer;
import eu.europa.esig.dss.pdf.pdfbox.visible.ImageRotationUtils;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxNativeFont;
import eu.europa.esig.dss.pdf.visible.CommonDrawerUtils;
import eu.europa.esig.dss.pdf.visible.ImageUtils;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDColorSpace;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureOptions;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * The native PDFBox signature drawer.
 * Creates text in the native way.
 */
public class NativePdfBoxVisibleSignatureDrawer extends AbstractPdfBoxSignatureDrawer {

	private static final Logger LOG = LoggerFactory.getLogger(NativePdfBoxVisibleSignatureDrawer.class);

	/** PDFBox font */
	private PDFont pdFont;

	/** Defines signature field dimensions and position */
	private SignatureFieldDimensionAndPosition dimensionAndPosition;

	/** Defines the default value for a non-transparent alpha layer */
	private static final float OPAQUE_VALUE = 0xff;

	@Override
	public void init(SignatureImageParameters parameters, PDDocument document, SignatureOptions signatureOptions)
			throws IOException {
		super.init(parameters, document, signatureOptions);
		if (!parameters.getTextParameters().isEmpty()) {
			this.pdFont = initFont();
		}
	}

	/**
	 * Method to initialize the specific font for PdfBox {@link PDFont}
	 */
	private PDFont initFont() throws IOException {
		DSSFont dssFont = parameters.getTextParameters().getFont();
		if (dssFont instanceof PdfBoxNativeFont) {
			PdfBoxNativeFont nativeFont = (PdfBoxNativeFont) dssFont;
			return nativeFont.getFont();
		} else if (dssFont instanceof DSSFileFont) {
			DSSFileFont fileFont = (DSSFileFont) dssFont;
			try (InputStream is = fileFont.getInputStream()) {
				return PDType0Font.load(document, is);
			}
		} else {
			return PdfBoxFontMapper.getPDFont(dssFont.getJavaFont());
		}
	}

	@Override
	public SignatureFieldDimensionAndPosition buildSignatureFieldBox() throws IOException {
		if (dimensionAndPosition == null) {
			PDPage originalPage = document
					.getPage(parameters.getFieldParameters().getPage() - ImageUtils.DEFAULT_FIRST_PAGE);
			SignatureFieldDimensionAndPositionBuilder dimensionAndPositionBuilder = new SignatureFieldDimensionAndPositionBuilder(
					parameters, originalPage, pdFont);
			dimensionAndPosition = dimensionAndPositionBuilder.build();
		}
		return dimensionAndPosition;
	}

	@Override
	public void draw() throws IOException {
		try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

			int pageNumber = parameters.getFieldParameters().getPage() - ImageUtils.DEFAULT_FIRST_PAGE;
			PDPage originalPage = document.getPage(pageNumber);
			SignatureFieldDimensionAndPosition dimensionAndPosition = buildSignatureFieldBox();
			// create a new page
			PDPage page = new PDPage(originalPage.getMediaBox());
			doc.addPage(page);
			PDAcroForm acroForm = new PDAcroForm(doc);
			doc.getDocumentCatalog().setAcroForm(acroForm);
			PDSignatureField signatureField = new PDSignatureField(acroForm);
			PDAnnotationWidget widget = signatureField.getWidgets().get(0);
			List<PDField> acroFormFields = acroForm.getFields();
			acroForm.setSignaturesExist(true);
			acroForm.setAppendOnly(true);
			acroForm.getCOSObject().setDirect(true);
			acroFormFields.add(signatureField);

			PDRectangle rectangle = getPdRectangle(dimensionAndPosition, page);
			widget.setRectangle(rectangle);

			PDStream stream = new PDStream(doc);
			PDFormXObject form = new PDFormXObject(stream);
			PDResources res = new PDResources();
			form.setResources(res);
			form.setFormType(1);

			form.setBBox(new PDRectangle(rectangle.getWidth(), rectangle.getHeight()));

			PDAppearanceDictionary appearance = new PDAppearanceDictionary();
			appearance.getCOSObject().setDirect(true);
			PDAppearanceStream appearanceStream = new PDAppearanceStream(form.getCOSObject());
			appearance.setNormalAppearance(appearanceStream);
			widget.setAppearance(appearance);

			try (PDPageContentStream cs = new PDPageContentStream(doc, appearanceStream)) {
				rotateSignature(cs, rectangle, dimensionAndPosition);
				setFieldBackground(cs, parameters.getBackgroundColor());
				setText(cs, dimensionAndPosition, parameters);
				setImage(cs, doc, dimensionAndPosition, parameters.getImage());
			}

			doc.save(baos);

			try (ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray())) {
				signatureOptions.setVisualSignature(bais);
				signatureOptions.setPage(pageNumber);
			}

		}
	}

	private void rotateSignature(PDPageContentStream cs, PDRectangle rectangle,
			SignatureFieldDimensionAndPosition dimensionAndPosition) throws IOException {
		switch (dimensionAndPosition.getGlobalRotation()) {
		case ImageRotationUtils.ANGLE_90:
			// pdfbox rotates in the opposite way
			cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_270), 0, 0));
			cs.transform(Matrix.getTranslateInstance(-rectangle.getHeight(), 0));
			break;
		case ImageRotationUtils.ANGLE_180:
			cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_180), 0, 0));
			cs.transform(Matrix.getTranslateInstance(-rectangle.getWidth(), -rectangle.getHeight()));
			break;
		case ImageRotationUtils.ANGLE_270:
			cs.transform(Matrix.getRotateInstance(Math.toRadians(ImageRotationUtils.ANGLE_90), 0, 0));
			cs.transform(Matrix.getTranslateInstance(0, -rectangle.getWidth()));
			break;
		case ImageRotationUtils.ANGLE_360:
			// do nothing
			break;
		default:
			throw new IllegalStateException(ImageRotationUtils.SUPPORTED_ANGLES_ERROR_MESSAGE);
		}
	}

	/**
	 * Fills a signature field background with the given color
	 * 
	 * @param cs    current {@link PDPageContentStream}
	 * @param color {@link Color} background color
	 * @throws IOException in case of error
	 */
	private void setFieldBackground(PDPageContentStream cs, Color color) throws IOException {
		setBackground(cs, color, new PDRectangle(-5000, -5000, 10000, 10000));
	}

	private void setBackground(PDPageContentStream cs, Color color, PDRectangle rect) throws IOException {
		if (color != null) {
			setAlphaChannel(cs, color);
			cs.setNonStrokingColor(color);
			// fill a whole box with the background color
			cs.addRect(rect.getLowerLeftX(), rect.getLowerLeftY(), rect.getWidth(), rect.getHeight());
			cs.fill();
			cleanTransparency(cs);
		}
	}

	/**
	 * Draws the given image with specified dimension and position
	 * 
	 * @param cs                   {@link PDPageContentStream} current stream
	 * @param doc                  {@link PDDocument} to draw the picture on
	 * @param dimensionAndPosition {@link SignatureFieldDimensionAndPosition} size
	 *                             and position to place the picture to
	 * @param image                {@link DSSDocument} image to draw
	 * @throws IOException in case of error
	 */
	private void setImage(PDPageContentStream cs, PDDocument doc,
			SignatureFieldDimensionAndPosition dimensionAndPosition, DSSDocument image) throws IOException {
		if (image != null) {
			try (InputStream is = image.openStream()) {
				cs.saveGraphicsState();
				byte[] bytes = IOUtils.toByteArray(is);
				PDImageXObject imageXObject = PDImageXObject.createFromByteArray(doc, bytes, image.getName());

				// divide to scale factor, because PdfBox due to the matrix transformation also
				// changes position parameters of the image
				float xAxis = dimensionAndPosition.getImageX();
				float yAxis = dimensionAndPosition.getImageY();
				float width = dimensionAndPosition.getImageWidth();
				float height = dimensionAndPosition.getImageHeight();
				if (!parameters.getTextParameters().isEmpty()) {
					xAxis *= dimensionAndPosition.getxDpiRatio();
					yAxis *= dimensionAndPosition.getyDpiRatio();
					width *= dimensionAndPosition.getxDpiRatio();
					height *= dimensionAndPosition.getyDpiRatio();
				}

				cs.drawImage(imageXObject, xAxis, yAxis, width, height);
				cs.transform(Matrix.getRotateInstance(
						((double) 360 - ImageRotationUtils.getRotation(parameters.getRotation())), width, height));

				cs.restoreGraphicsState();
			}
		}
	}

	/**
	 * Draws a custom text with the specified parameters
	 * 
	 * @param cs                   {@link PDPageContentStream} current stream
	 * @param dimensionAndPosition {@link SignatureFieldDimensionAndPosition} size
	 *                             and position to place the text to
	 * @param parameters       {@link SignatureImageTextParameters} text to
	 *                             place on the signature field
	 * @throws IOException in case of error
	 */
	private void setText(PDPageContentStream cs, SignatureFieldDimensionAndPosition dimensionAndPosition,
			SignatureImageParameters parameters) throws IOException {
		SignatureImageTextParameters textParameters = parameters.getTextParameters();
		if (!textParameters.isEmpty()) {
			setTextBackground(cs, textParameters, dimensionAndPosition);
			DSSFont dssFont = textParameters.getFont();
			float fontSize = dssFont.getSize();
			fontSize *= ImageUtils.getScaleFactor(parameters.getZoom());
			cs.beginText();
			cs.setFont(pdFont, fontSize);
			cs.setNonStrokingColor(textParameters.getTextColor());
			setAlphaChannel(cs, textParameters.getTextColor());

			PdfBoxFontMetrics pdfBoxFontMetrics = new PdfBoxFontMetrics(pdFont);

			String[] strings = pdfBoxFontMetrics.getLines(textParameters.getText());

			float properSize = CommonDrawerUtils.computeProperSize(textParameters.getFont().getSize(),
					parameters.getDpi());

			float fontHeight = pdfBoxFontMetrics.getHeight(textParameters.getText(), properSize);
			cs.setLeading(textSizeWithDpi(fontHeight, dimensionAndPosition.getyDpi()));

			cs.newLineAtOffset(dimensionAndPosition.getTextX(),
					// align vertical position
					dimensionAndPosition.getTextHeight() + dimensionAndPosition.getTextY() - fontSize);

			float previousOffset = 0;
			for (String str : strings) {
				float stringWidth = pdfBoxFontMetrics.getWidth(str, fontSize);
				float offsetX = 0;
				switch (textParameters.getSignerTextHorizontalAlignment()) {
				case RIGHT:
					offsetX = dimensionAndPosition.getTextWidth() - stringWidth
							- textSizeWithDpi(textParameters.getPadding() * 2, dimensionAndPosition.getxDpi())
							- previousOffset;
					break;
				case CENTER:
					offsetX = (dimensionAndPosition.getTextWidth() - stringWidth) / 2
							- textSizeWithDpi(textParameters.getPadding(), dimensionAndPosition.getxDpi())
							- previousOffset;
					break;
				default:
					break;
				}
				previousOffset += offsetX;
				cs.newLineAtOffset(offsetX, 0); // relative offset
				cs.showText(str);
				cs.newLine();
			}
			cs.endText();
			cleanTransparency(cs);
		}
	}

	private void setTextBackground(PDPageContentStream cs, SignatureImageTextParameters textParameters,
			SignatureFieldDimensionAndPosition dimensionAndPosition) throws IOException {
		if (textParameters.getBackgroundColor() != null) {
			PDRectangle rect = new PDRectangle(
					dimensionAndPosition.getTextX()
							- textSizeWithDpi(textParameters.getPadding(), dimensionAndPosition.getxDpi()),
					dimensionAndPosition.getTextY()
							+ textSizeWithDpi(textParameters.getPadding(), dimensionAndPosition.getyDpi()),
					dimensionAndPosition.getTextWidth(), dimensionAndPosition.getTextHeight());
			setBackground(cs, textParameters.getBackgroundColor(), rect);
		}
	}

	private float textSizeWithDpi(float size, int dpi) {
		return CommonDrawerUtils.toDpiAxisPoint(size / CommonDrawerUtils.getTextScaleFactor(dpi), dpi)
				* CommonDrawerUtils.getTextScaleFactor(parameters.getDpi());
	}

	/**
	 * Sets alpha channel if needed
	 * 
	 * @param cs    {@link PDPageContentStream} current stream
	 * @param color {@link Color}
	 * @throws IOException in case of error
	 */
	private void setAlphaChannel(PDPageContentStream cs, Color color) throws IOException {
		// if alpha value is less then 255 (is transparent)
		float alpha = color.getAlpha();
		if (alpha < OPAQUE_VALUE) {
			LOG.warn("Transparency detected and enabled (Be aware: not valid with PDF/A !)");
			setAlpha(cs, alpha);
		}
	}

	private void setAlpha(PDPageContentStream cs, float alpha) throws IOException {
		PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
		gs.setNonStrokingAlphaConstant(alpha / OPAQUE_VALUE);
		cs.setGraphicsStateParameters(gs);
	}

	private void cleanTransparency(PDPageContentStream cs) throws IOException {
		setAlpha(cs, OPAQUE_VALUE);
	}

	/**
	 * Returns {@link PDRectangle} of the widget to place on page
	 * 
	 * @param dimensionAndPosition {@link SignatureFieldDimensionAndPosition}
	 *                             specifies widget size and position
	 * @param page                 {@link PDPage} to place the widget on
	 * @return {@link PDRectangle}
	 */
	private PDRectangle getPdRectangle(SignatureFieldDimensionAndPosition dimensionAndPosition, PDPage page) {
		PDRectangle pageRect = page.getMediaBox();
		PDRectangle pdRectangle = new PDRectangle();
		pdRectangle.setLowerLeftX(dimensionAndPosition.getBoxX());
		// because PDF starts to count from bottom
		pdRectangle.setLowerLeftY(
				pageRect.getHeight() - dimensionAndPosition.getBoxY() - dimensionAndPosition.getBoxHeight());
		pdRectangle.setUpperRightX(dimensionAndPosition.getBoxX() + dimensionAndPosition.getBoxWidth());
		pdRectangle.setUpperRightY(pageRect.getHeight() - dimensionAndPosition.getBoxY());
		return pdRectangle;
	}

	@Override
	protected String getColorSpaceName(DSSDocument image) throws IOException {
		try (InputStream is = image.openStream()) {
			byte[] bytes = IOUtils.toByteArray(is);
			PDImageXObject imageXObject = PDImageXObject.createFromByteArray(document, bytes, image.getName());
			PDColorSpace colorSpace = imageXObject.getColorSpace();
			return colorSpace.getName();
		}
	}

}
