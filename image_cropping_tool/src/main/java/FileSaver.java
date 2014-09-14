import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class FileSaver
{
    private String name;
    private String suffix;
    private String fileName;
    private BufferedImage image;

    public FileSaver(BufferedImage image, String name)
    {
        this.suffix = ".jpg";
        this.name = name;
        this.fileName = name + suffix;
        this.image = image;
    }

    public void save()
    {
        File file = generateUniqueFileName(new File(fileName));
        try {
            ImageIO.write(image, "jpg", file);
        } catch (IOException ioex) {

        }
    }

    private File generateUniqueFileName(File file)
    {
        if (file.exists())
        {
            String filePath = FilenameUtils.getFullPathNoEndSeparator(file.getAbsolutePath());
            File parent = new File(filePath);
            String[] files = parent.list(new FilenameFilter() {
                            @Override
                            public boolean accept(final File dir, final String fileName)
                            {
                                return fileName.matches(name + "\\d*" + suffix);
                            }
            });
            return new File(name + Integer.toString(files.length + 1) + suffix);
        }
        return file;
    }
}
