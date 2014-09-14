package text_segmentation;

import java.awt.image.BufferedImage;

import java.awt.Color;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.neuroph.imgrec.ImageUtilities;



public class CharExtractor {

    private int cropTopY = 0;//up locked coordinate
    private int cropBottomY = 0;//down locked coordinate
    private int cropLeftX = 0;//left locked coordinate
    private int cropRightX = 0;//right locked coordinate
    private BufferedImage imageWithChars = null;
    private boolean endOfImage;//end of picture
    private boolean endOfRow;//end of current reading row

    private String dir;

    /**
     * Creates new char extractor with soecified text image
     * @param imageWithChars - image with text
     */
    public CharExtractor(BufferedImage imageWithChars) {
        this.imageWithChars = imageWithChars;
    }

    public void setImageWithChars(BufferedImage imageWithChars) {
        this.imageWithChars = imageWithChars;
    }

    private boolean isWhite(Color p) {
        return ((p.getRed()>=235)&&(p.getBlue()>=235)&&(p.getGreen()>=235));
    }
    private boolean isBlack(Color p) {
        return ((p.getRed()<=20)&&(p.getGreen()<=20)&&(p.getBlue()<=20));
    }

    /**
     * This method scans image pixels until it finds the first black pixel (TODO: use         foreground color which is black by default).
     * When it finds black pixel, it sets cropTopY and returns true. if it reaches end of image and does not find black pixels,
     * it sets endOfImage flag and returns false.
     * @return - returns true when black pixel is found and cropTopY value is changed, and false if cropTopY value is not changed
     */
    private boolean findCropTopY() {
        for (int y = cropBottomY; y < imageWithChars.getHeight(); y++) { // why cropYDown? -   for multiple lines of text using cropBottomY from previous line above; for first line its zero
            for (int x = cropLeftX; x < imageWithChars.getWidth(); x++) { // scan starting from the previous left crop position - or it shoud be right???

                if (isBlack(new Color(imageWithChars.getRGB(x, y)))) { // if its black rixel (also consider condition close to black or not white or different from background)
                    this.cropTopY = y;   // save the current y coordiante
                    return true;        // and return true
                }
            }
        }
        endOfImage = true;  //sets this flag if no black pixels are found
        return false;       // and return false
    }

    /**
     * This method scans image pixels until it finds first row with white pixels. (TODO: background color which is white by default).
     * When it finds line with all white pixels, it sets cropBottomY and returns true
     * @return - returns true when cropBottomY value is set, false otherwise
     */

    private boolean findCropBottomY() {
        for (int y = cropTopY; y < imageWithChars.getHeight(); y++) { // scan image from  top to bottom
            int whitePixCounter = 0; //counter of white pixels in a row
            for (int x = cropLeftX; x < imageWithChars.getWidth(); x++) { // scan all pixels to right starting from left crop position

                if (isWhite(new Color(imageWithChars.getRGB(x, y)))){    // if its white pixel
                    whitePixCounter++;                      // increase counter
                }
            }
            if (whitePixCounter == imageWithChars.getWidth()) { // if we have reached end of line counting white pixels (x pos)
                cropBottomY = y;// that means that we've found white line, so set current y coordinate minus 1
                return true; // as cropBottomY and finnish with true
            }
            if (y == imageWithChars.getHeight() - 1) {  // if we have reached end of image
                cropBottomY = y;                        // set crop bottom
                endOfImage = true;                      // set corresponding endOfImage flag
                return true;                            // and return true
            }
        }
        return false;                                   // this should never happen, however its possible if image has non white bg
    }

    private boolean findCropLeftX() {
        //int whitePixCounter = 0;                                            // white pixel counter between the letters
        for (int x = cropRightX; x < imageWithChars.getWidth(); x++) {      // start from previous righ crop position (previous letter), and scan following pixels to the right
            for (int y = cropTopY; y <= cropBottomY; y++) {             // vertical pixel scan at current x coordinate

                if (isBlack(new Color(imageWithChars.getRGB(x, y))))  {             // when we find black pixel
                    cropLeftX = x;                                          // set cropLeftX
                    return true;                                            // and return true
                }
            }

            // BUG?: this condition looks strange.... we might not need whitePixCounter at all, it might be used for 'I' letter
            //whitePixCounter++;                                              // if its not black pixel assume that its white pixel
//        if (whitePixCounter == 3) {                                     // why 3 pixels? its hard coded for some case and does not work in general...!!!
//            whitePixCounter = 0;                                        // why does it sets to zero, this has no purporse at all...
//        }
        }
        endOfRow = true;        // if we have reached end of row and we have not found black pixels, set the endOfRow flag
        return false;           // and return false
    }

    /**
     * This method scans image pixels to the right until it finds next row where all pixel are white, y1 and y2.
     * @return - return true  when x2 value is changed and false when x2 value is not changed
     */
    private boolean findCropRightX() {
        for (int x = cropLeftX; x < imageWithChars.getWidth(); x++) {   // start from current cropLeftX position and scan pixels to the right
            int whitePixCounter = 0;
            for (int y = cropTopY; y <= cropBottomY; y++) {             // vertical pixel scan at current x coordinate
                if (isWhite(new Color(imageWithChars.getRGB(x, y)))) {                    // if we have white pixel at current (x, y)
                    whitePixCounter++;                                      // increase whitePixCounter
                }
            }

            // this is for space!
            int heightPixels = cropBottomY - cropTopY + 1;                      // calculate crop height
            if (whitePixCounter == heightPixels) {                         // if white pixel count is equal to crop height+1  then this is white vertical line, means end of current char/ (+1 is for case when there is only 1 pixel; a 'W' bug fix)
                cropRightX = x;                                             // so set cropRightX
                return true;                                                // and return true
            }

            // why we need this when we allready have condiiton in the for loop? - for the last letter in the row.
            if (x == imageWithChars.getWidth() - 1) {                       // if we have reached end of row with x position
                cropRightX = x;                                             // set cropRightX
                endOfRow = true;                                            // set endOfRow flag
                return true;                                                // and return true
            }
        }
        return true;
    }

    public List<BufferedImage> extractCharImagesToRecognize() {
        List<BufferedImage> trimedImages = new ArrayList<BufferedImage>();
        //int i = 0;

        while (endOfImage == false) {
            endOfRow = false;
            boolean foundTop = findCropTopY();
            boolean foundBottom = false;
            if (foundTop == true) {
                foundBottom = findCropBottomY();
                if (foundBottom == true) {
                    while (endOfRow == false) {
                        boolean foundLeft = false;
                        boolean foundRight = false;
                        foundLeft = findCropLeftX();
                        if (foundLeft == true) {
                            foundRight = findCropRightX();
                            if (foundRight == true) {

                                int cropLeftX1 = (cropLeftX == 0) ? cropLeftX : cropLeftX-1;
                                int cropTopY1 = (cropTopY == 0) ? cropTopY : cropTopY-1;
                                int cropRightX1 = (cropRightX == imageWithChars.getWidth() - 1) ? cropRightX : cropRightX+1;
                                int cropBottomY1 = (cropBottomY == imageWithChars.getHeight() - 1) ? cropBottomY : cropBottomY+1;


                                BufferedImage image =
                                    ImageUtilities.trimImage(ImageUtilities.cropImage(imageWithChars,
                                    cropLeftX1, cropTopY1, cropRightX1, cropBottomY1));

                                int height1 = cropBottomY-cropTopY+1;
                                int widgh1 = cropRightX-cropLeftX+1;
                                if ((height1 >= 3) && (widgh1 >= 3))
                                {
                                    trimedImages.add(image);
                                    //i++;
                                }
                            }
                        }
                    }
                    cropLeftX = 0;
                    cropRightX = 0;
                }
            }
        }
        cropTopY = 0;
        cropBottomY = 0;
        endOfImage = false;

        return trimedImages;
    }

    public static void segmentation(File inputFile, String outputDir) {
        try {
            //проверяем, что если файл не существует то создаем его

            BufferedImage img = ImageIO.read(inputFile);
            CharExtractor ch=new CharExtractor(img);
            List<BufferedImage> list=ch.extractCharImagesToRecognize();
            for(int i=0;i<list.size();i++)
            {
                File outputfile = new File(outputDir+"//char_" +i+ ".png");
                ImageIO.write(list.get(i),"png", outputfile);
            }

        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void main(String[] args) throws Exception {
        File f=new File("Input//5.jpg");
        segmentation(f,"Output");

        /*
        BufferedImage img=ImageIO.read(f);
        CharExtractor ch=new CharExtractor(img);
        List<BufferedImage> list=ch.extractCharImagesToRecognize();

        for(int i=0;i<list.size();i++)
        {
            File outputfile = new File("Output//char_" +i+ ".png");
            ImageIO.write(list.get(i),"png", outputfile);
        }
        */
    }
}