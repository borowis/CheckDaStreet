import junit.framework.Assert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class BitmapResizerTest {

    @org.testng.annotations.Test
    public void testFitToSize() throws Exception {

        BufferedImage incomeImage = ImageIO.read(new File(getClass().getResource("/avatar100x100.gif").toURI()));

        BufferedImage outImage = BitmapResizer.fitToSize(incomeImage);

        ImageIO.write(outImage, "gif", new File("/Users/admin/Documents/programming/Java/LinguaHack/BitmapResize/src/main/resources/avatar60x60_WITHmagic.gif"));

        Assert.assertEquals(outImage.getHeight(), 60);
        Assert.assertEquals(outImage.getWidth(), 60);


    }
}