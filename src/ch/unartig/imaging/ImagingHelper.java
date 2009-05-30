package ch.unartig.imaging;

import javax.imageio.ImageIO;
import java.io.*;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.*;
import java.net.URL;
import com.sun.media.jai.codec.FileSeekableStream;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;

import javax.media.jai.JAI;
import javax.media.jai.KernelJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.BorderExtender;
import javax.media.jai.widget.ScrollingImagePanel;
import java.util.logging.Logger;


/**
 *
 */
public class ImagingHelper
{
    public static final float _IMAGE_QUALITY_STANDARD = 0.75F;
    public static final float _SHARP_FACTOR_STANDARD = 0.5F;



    static Logger _logger = Logger.getLogger(ImagingHelper.class.getName());
    // todo add to resource file, application config
    private static final String _RESOURCE_WATERMARK_LANDSCAPE = "/images/watermark2_quer.png";
    private static final String _RESOURCE_WATERMARK_PORTRAIT = "/images/watermark2_hoch.png";
    private static float imageSharpFactor = _SHARP_FACTOR_STANDARD;
    private static float imageQuality = _IMAGE_QUALITY_STANDARD;

    /**
     * loads an image from disk and returns a RenderedOp
     *
     * @param file
     * @return image
     *          File not found or similar problem
     * @throws UnartigImagingException
     */
    public static RenderedOp load(File file) throws UnartigImagingException
    {

        RenderedOp retVal;
        FileSeekableStream stream;
        try
        {
            stream = new FileSeekableStream(file);
        } catch (IOException e)
        {
//            _logger.error("Exception while loading image from file", e);
            throw new UnartigImagingException("Exception while loading image from file", e);
        }
        /* Create an operator to decode the image file. */
        // add border extender here?
        retVal = JAI.create("stream", stream);
        return retVal;
    }

    public static RenderedOp readImage(InputStream stream)
    {

        return JAI.create("stream", stream);
    }

    /**
     * @param sampledOp
     * @param os        output stream for encoder
     * @param quality
     */
    private static void renderJpg(RenderedOp sampledOp, OutputStream os, float quality)
    {
        // todo : robust exception handling
        JPEGEncodeParam encParam = new JPEGEncodeParam();
        try
        {
            encParam.setQuality(quality);
            ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", os, encParam);
            encoder.encode(sampledOp);
//            _logger.debug("ImagingHelper.saveJpg : image encoded");
        } catch (FileNotFoundException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }


    /**
     * Using the renderedOP save the image as a jpg file.
     *
     * @param image
     * @param quality : A setting of 0.0 produces the highest compression ratio, with a sacrifice to image quality. The default value is 0.75
     * @param file    File to save
     * @param applyWatermark set to true to apply a watermark over the resulting image
     * @return true for success
     */
    public static boolean saveJpg(RenderedOp image, float quality, File file, boolean applyWatermark)
    {
        try
        {
            BufferedImage sourceImage = image.getAsBufferedImage();

            int sourceWidth = sourceImage.getWidth();
            int sourceHeight = sourceImage.getHeight();
            BufferedImage result = new BufferedImage(sourceWidth, sourceHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics2D = result.createGraphics();

            graphics2D.drawImage(sourceImage, 0, 0, null);
//            _logger.debug("Using a watermark ??? : " + applyWatermark);
            if (applyWatermark)
            {
                graphics2D.drawImage(getWatermark(sourceWidth,sourceHeight), 0, 0, null);
            }
            ImageIO.write(result, "jpg", file);

        } catch (FileNotFoundException e)
        {
            e.printStackTrace();
            return false;
        } catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Return a alpha-transparent Watermark in either landscape or portrait orientation, depending on width and height of the source
     * @param sourceWidth in Pixels ?
     * @param sourceHeight in Pixels ?
     * @return Alpha-Transparent Image to overlay with a source image.
     * @throws IOException
     */
    private static BufferedImage getWatermark(int sourceWidth, int sourceHeight) throws IOException
    {
         if (sourceWidth > sourceHeight)
        {
//            _logger.debug("Trying to read landscape watermark image.... ");
            URL test = ImagingHelper.class.getResource(_RESOURCE_WATERMARK_LANDSCAPE);
//            _logger.debug("Resource as File :  Path : " + test.getFile());
            InputStream is = ImagingHelper.class.getResourceAsStream(_RESOURCE_WATERMARK_LANDSCAPE);
            BufferedImage bufferedImage = ImageIO.read(is);
//            _logger.debug("Read landscape watermark image!!");
            return bufferedImage;
        } else
        {
//            _logger.debug("Trying to read portrait watermark image.... ");
            URL test = ImagingHelper.class.getResource(_RESOURCE_WATERMARK_PORTRAIT);
//            _logger.debug("Resource as File :  Path : " + test.getFile());
            InputStream is = ImagingHelper.class.getResourceAsStream(_RESOURCE_WATERMARK_PORTRAIT);
            BufferedImage bufferedImage = ImageIO.read(is);
//            _logger.debug("Read portrait watermark image!!");
            return bufferedImage;
        }
    }

    /**
     * Todo error handling ! For example, if a small image is rescaled, an exception might happen ... try with an index pic
     *
     * @param image
     * @param scaleFactor
     * @return image
     */
    public static RenderedOp reSample(RenderedOp image, Double scaleFactor)
    {

        ParameterBlock params = new ParameterBlock();
        // Rendering Hint needed in order to avoid a black border around the image.
        RenderingHints renderingHints  = new RenderingHints(null);
        renderingHints.put(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        image.setRenderingHint(JAI.KEY_BORDER_EXTENDER,BorderExtender.createInstance(BorderExtender.BORDER_COPY));
//        _logger.debug("renderingHints = " + renderingHints);
        params.addSource(image);
        params.add(scaleFactor); // scale factor
        /* Create an operator to scale image. */
        return JAI.create("subsampleaverage", params,renderingHints);
    }

    /**
     * use unsharpen operation on the renderedOp
     *
     * @param image
     * @param factor = 0 : no effect;  factor> 0 : sharpening;factor -1 < gain < 0 : smoothing
     * @return a renderedOp
     */
    public static RenderedOp unSharpen(RenderedOp image, float factor)
    {

        float[] fA = new float[4];
        KernelJAI kernel = new KernelJAI(2, 2, fA);

        ParameterBlock unsharpParams = new ParameterBlock();
        unsharpParams.addSource(image);
        unsharpParams.add(null); // kernel: 3x3 average
        unsharpParams.add(factor);
        // rendering hint border copy to avoid black frame!!
        return JAI.create("unsharpMask", unsharpParams,new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY)));
//        return image;
    }

    public static void display(RenderedOp image)
    {
/* Get the width and height of image2. */
        int width = image.getWidth();
        int height = image.getHeight();

        ScrollingImagePanel panel = new ScrollingImagePanel(image, width, height);
        Frame window = new Frame("JAI Sample Program");
        window.add(panel);
        window.pack();
        window.show();
    }

    /**
     * @param renderedOp
     * @return max width or hight of picture
     */
    public static int getMaxWidthOrHightOf(RenderedOp renderedOp)
    {
        return Math.max(renderedOp.getHeight(), renderedOp.getWidth());
    }

    /**
     * Creates a new image based on the passed renderedOp.
     *
     * @param fineImage        the source for the new image
     * @param scale
     * @param newImageFile
     * @param quality
     * @param imageSharpFactor
     * @param applyWatermark
     */
    public static void createNewImage(RenderedOp fineImage, Double scale, File newImageFile, float quality, float imageSharpFactor, boolean applyWatermark)
    {
//        _logger.debug("Going to create new Image [" + newImageFile.getName() + "]from File : " + fineImage.toString() + " using scale :  " + scale);
        RenderedOp sharpThumbImage;
        RenderedOp thumbImage;
        try
        {
            thumbImage = reSample(fineImage, scale);
            sharpThumbImage = unSharpen(thumbImage, imageSharpFactor);
        } catch (Exception e)
        {
//            _logger.info("rendering threw exception. probably rendering result is bigger than original; using original image instead");
//            _logger.debug("Exception: ", e);
            sharpThumbImage = fineImage;
        }
        // todo check return value; report problem images
        saveJpg(sharpThumbImage, quality, newImageFile, applyWatermark);
    }

    /**
     * todo: this does not perform well ... find a better method to find photo dimensions (external EXIF library ?)
     *
     * @param photoFile
     * @return # of width-pixels for passed photo
     */
    public static Integer getPixelsWidth(File photoFile) throws UnartigImagingException
    {
        return load(photoFile).getWidth();
    }

    public static Integer getPixelsHeight(File photoFile) throws UnartigImagingException
    {
        return load(photoFile).getHeight();
    }

    /**
     * generic resample function
     *
     * @param file           the file to resample
     * @param resampleFactor
     * @param os             OutputStream
     * @param quality
     *          from load; file not found or similar
     * @throws UnartigImagingException
     */
    public static void reSample(File file, Double resampleFactor, OutputStream os, float quality) throws UnartigImagingException
    {
//        PipedOutputStream retVal = new PipedOutputStream();
        RenderedOp sampledOp = reSample(load(file), resampleFactor);
        renderJpg(sampledOp, os, quality);
    }

    /**
     * Given an Image, create a scaled copy from that image.
     *
     * @param newImageFileName Filename of the new image to be created (only file name, no path)
     * @param sourceImage      Source photo rendered op
     * @param longerSidePixels target image longer side in pixels
     * @param path             Path to create new image in
     * @param applyWatermark set to true if watermarks are laid over the new image (for display images)
     */
    public static void createScaledImage(String newImageFileName, RenderedOp sourceImage, double longerSidePixels, File path, boolean applyWatermark) {
        Double scale;
        scale = longerSidePixels / (double) ImagingHelper.getMaxWidthOrHightOf(sourceImage);
        File newFile = new File(path, newImageFileName);
        createNewImage(sourceImage, scale, newFile, imageQuality, imageSharpFactor, applyWatermark);
//        _logger.info("wrote new file " + newFile.getAbsolutePath());
    }

    /**
     * Given an Image, create a scaled copy from that image.
     *
     * @param newImageFileName Filename of the new image to be created (only file name, no path)
     * @param sourceImage
     * @param sourceMaxWidthOrHight
     * @param longerSidePixels target image longer side in pixels
     * @param pathToNewImage             Path to create new image in
     * @param applyWatermark set to true if watermarks are laid over the new image (for display images)
     */
    public static void createScaledImage(String newImageFileName, final File sourceImage, int sourceMaxWidthOrHight, double longerSidePixels, File pathToNewImage, boolean applyWatermark) {
        Double scale;
        scale = longerSidePixels / (double) sourceMaxWidthOrHight;
        File newFile = new File(pathToNewImage, newImageFileName);
        try {
            createNewImage(load(sourceImage), scale, newFile, imageQuality, imageSharpFactor, applyWatermark);
        } catch (UnartigImagingException e) {

            e.printStackTrace();
            throw new RuntimeException();
        }
//        _logger.info("wrote new file " + newFile.getAbsolutePath());
    }
}
