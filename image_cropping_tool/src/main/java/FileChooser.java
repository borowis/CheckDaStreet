import java.awt.im.InputContext;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.SwingUtilities;

/*
 * FileChooserDemo.java uses these files:
 *   images/Open16.gif
 *   images/Save16.gif
 */
public class FileChooser extends JPanel
        implements ActionListener {
    static private final String newline = "\n";
    JButton openButton;
    JTextArea log;
    JFileChooser fc;
    JTextField chooserWidth;
    JTextField chooserHeight;

    public FileChooser() {
        super(new BorderLayout());

        //Create the log first, because the action listeners
        //need to refer to it.
        log = new JTextArea(5,20);
        log.setMargin(new Insets(5,5,5,5));
        log.setEditable(false);
        JScrollPane logScrollPane = new JScrollPane(log);

        //Create a file chooser
        fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);

        //Uncomment one of the following lines to try a different
        //file selection mode.  The first allows just directories
        //to be selected (and, at least in the Java look and feel,
        //shown).  The second allows both files and directories
        //to be selected.  If you leave these lines commented out,
        //then the default mode (FILES_ONLY) will be used.
        //
        //fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        //fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

        //Create the open button.  We use the image from the JLF
        //Graphics Repository (but we extracted it from the jar).
        openButton = new JButton("Open a File...");
        openButton.addActionListener(this);

        //For layout purposes, put the buttons in a separate panel
        JPanel buttonPanel = new JPanel(); //use FlowLayout
        buttonPanel.add(openButton);

        chooserHeight = new JTextField(5);
        chooserWidth = new JTextField(5);

        JPanel chooserPanel = new JPanel();
        chooserPanel.add(chooserWidth);
        chooserPanel.add(chooserHeight);

        //Add the buttons and the log to this panel.
        add(buttonPanel, BorderLayout.PAGE_START);
        add(logScrollPane, BorderLayout.CENTER);
        add(chooserPanel, BorderLayout.PAGE_END);
    }

    public void actionPerformed(ActionEvent e) {

        try
        {
            //Handle open button action.
            if (e.getSource() == openButton)
            {
                int returnVal = fc.showOpenDialog(FileChooser.this);

                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    for (File file : fc.getSelectedFiles())
                    {
                        //This is where a real application would open the file.
                        log.append("Opening: " + file.getName() + "." + newline);

                        Cropping cropping = new Cropping(ImageIO.read(file), Integer.parseInt(chooserWidth.getText()), Integer.parseInt(chooserHeight.getText()));
                        ClipMover clipMover = new ClipMover(cropping);

                        cropping.addMouseListener(clipMover);
                        cropping.addMouseMotionListener(clipMover);

                        JFrame f = new JFrame();
                        f.getContentPane().add(new JScrollPane(cropping));
                        f.getContentPane().add(cropping.getUIPanel(), "South");
                        f.setSize(700, 800);
                        f.setLocation(200,200);
                        f.setVisible(true);
                    }
                }
                else
                {
                    log.append("Open command cancelled by user." + newline);
                }
                log.setCaretPosition(log.getDocument().getLength());
            }
        }
        catch (IOException ioex)
        {

        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("FileChooserDemo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //Add content to the window.
        frame.add(new FileChooser());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
                                       public void run() {
                                           //Turn off metal's use of bold fonts
                                           UIManager.put("swing.boldMetal", Boolean.FALSE);
                                           createAndShowGUI();
                                       }
                                   });

        AWTEventListener awtWindowListener = new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (event instanceof WindowEvent) {
                    if (WindowEvent.WINDOW_CLOSED == event.getID()
                            || WindowEvent.WINDOW_CLOSING == event.getID()) {
                        Window child = ((WindowEvent) event).getWindow();
                        Window parent = SwingUtilities.getWindowAncestor(child);
                        if (parent == null) return;
                        InputContext childIC = child.getInputContext();
                        parent.getInputContext().selectInputMethod(childIC.getLocale());
                    }
                }

            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(awtWindowListener, AWTEvent.WINDOW_EVENT_MASK);
    }
}
