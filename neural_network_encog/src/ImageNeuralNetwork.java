import org.encog.EncogError;
import org.encog.engine.network.activation.ActivationElliott;
import org.encog.ml.data.folded.FoldedDataSet;
import org.encog.ml.train.strategy.Greedy;
import org.encog.ml.train.strategy.ResetStrategy;
import org.encog.ml.train.strategy.StopTrainingStrategy;
import org.encog.neural.data.NeuralData;
import org.encog.neural.data.basic.BasicNeuralData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.cross.CrossValidationKFold;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.neural.networks.training.strategy.RegularizationStrategy;
import org.encog.neural.networks.training.strategy.SmartLearningRate;
import org.encog.neural.networks.training.strategy.SmartMomentum;
import org.encog.neural.pattern.ADALINEPattern;
import org.encog.neural.pattern.ElmanPattern;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.platformspecific.j2se.TrainingDialog;
import org.encog.platformspecific.j2se.data.image.ImageMLData;
import org.encog.platformspecific.j2se.data.image.ImageMLDataSet;
import org.encog.util.Format;
import org.encog.util.downsample.Downsample;
import org.encog.util.downsample.RGBDownsample;
import org.encog.util.downsample.SimpleIntensityDownsample;
import org.encog.util.simple.EncogUtility;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageNeuralNetwork
{
    class ImagePair {
        private final File file;
        private final int  identity;

        public ImagePair(final File file, final int identity) {
            super();
            this.file = file;
            this.identity = identity;
        }
        public File getFile() {
            return this.file;
        }
        public int getIdentity() {
            return this.identity;
        }
    }

    public static void main(final String[] args) {
        if (args.length < 1) {
            System.out.println(
                    "Must specify command file. See source for format.");
        } else {
            try {
                final ImageNeuralNetwork program = new ImageNeuralNetwork();
                program.execute(args[0]);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    private final List<ImagePair>      imageList       = new ArrayList<ImagePair>();
    private final Map<String, String>  args            = new HashMap<String, String>();
    private final Map<String, Integer> identity2neuron = new HashMap<String, Integer>();
    private final Map<Integer, String> neuron2identity = new HashMap<Integer, String>();
    private ImageMLDataSet     training;
    private String             line;
    private int                outputCount;
    private int                downsampleWidth;
    private int                downsampleHeight;
    private BasicNetwork       network;
    private Downsample         downsample;

    private int assignIdentity(final String identity)
    {
        if (this.identity2neuron.containsKey(identity))
        {
            return this.identity2neuron.get(identity);
        }
        final int result = this.outputCount;
        this.identity2neuron.put(identity, result);
        this.neuron2identity.put(result, identity);
        this.outputCount++;
        return result;
    }

    public void execute(final String file) throws IOException
    {
        final FileInputStream fstream = new FileInputStream(file);
        final DataInputStream in = new DataInputStream(fstream);
        final BufferedReader br = new BufferedReader(new InputStreamReader(in));
        while ((this.line = br.readLine()) != null)
        {
            executeLine();
        }

        in.close();
    }

    private void executeCommand(final String command,
            final Map<String, String> args) throws IOException
    {
        if (command.equals("input"))
        {
            processInput();
        }
        else if (command.equals("createtraining"))
        {
            processCreateTraining();
        }
        else if (command.equals("train"))
        {
            processTrain();
        }
        else if (command.equals("network"))
        {
            processNetwork();
        }
        else if (command.equals("whatis"))
        {
            processWhatIs();
        }
        else if (command.equals("save"))
        {
            processSave();
        }
        else if (command.equals("restore"))
        {
            processRestore();
        }
    }

    public void executeLine() throws IOException
    {
        final int index = this.line.indexOf(':');
        if (index == -1)
        {
            throw new EncogError("Invalid command: " + this.line);
        }
        final String command = this.line.substring(0, index).toLowerCase().trim();
        final String argsStr = this.line.substring(index + 1).trim();
        final StringTokenizer tok = new StringTokenizer(argsStr, ",");
        this.args.clear();

        while (tok.hasMoreTokens()) {
            final String arg = tok.nextToken();
            final int index2 = arg.indexOf(':');
            if (index2 == -1) {
                throw new EncogError("Invalid command: " + this.line);
            }
            final String key = arg.substring(0, index2).toLowerCase().trim();
            final String value = arg.substring(index2 + 1).trim();
            this.args.put(key, value);
        }
        executeCommand(command, this.args);
    }
    private String getArg(final String name) {
        final String result = this.args.get(name);
        if (result == null) {
            throw new EncogError("Missing argument " + name
                    + " on line: " + this.line);
        }
        return result;
    }
    private void processCreateTraining() {
        final String strWidth = getArg("width");
        final String strHeight = getArg("height");
        final String strType = getArg("type");
        this.downsampleHeight = Integer.parseInt(strHeight);
        this.downsampleWidth = Integer.parseInt(strWidth);
        if (strType.equals("RGB")) {
            this.downsample = new RGBDownsample();
        } else {
            this.downsample = new SimpleIntensityDownsample();
        }

        this.training = new ImageMLDataSet(this.downsample, false, 1, -1);
        System.out.println("Training set created");
    }
    private void processInput() throws IOException {
        final String imageDir = getArg("directory");
        final String type = getArg("type");
        final Pattern p = Pattern.compile("(.)\\d*\\." + type);
        final File directory = new File(imageDir);
        for (String fileName : directory.list(new FilenameFilter() {
                                   @Override
                                   public boolean accept(final File dir, final String name)
                                   {
                                       return name.matches(".*\\." + type);
                                   }
                               })) {
            final Matcher m = p.matcher(fileName);
            if (m.matches())
            {
                final String identity = m.toMatchResult().group(1);
                final int idx = assignIdentity(identity);
                final File file = new File(directory, fileName);
                this.imageList.add(new ImagePair(file, idx));
                System.out.println("Added input image:" + fileName);
            }
        }
    }
    private void processNetwork() throws IOException {
        System.out.println("Downsampling images...");
        for (final ImagePair pair : this.imageList) {
            final BasicNeuralData ideal = new BasicNeuralData(this.outputCount);
            final int idx = pair.getIdentity();
            for (int i = 0; i < this.outputCount; i++) {
                if (i == idx) {
                    ideal.setData(i, 1);
                } else {
                    ideal.setData(i, -1);
                }
            }
            final Image img = ImageIO.read(pair.getFile());
            final ImageMLData data = new ImageMLData(img);
            this.training.add(data, ideal);
        }
        final String strHidden1 = getArg("hidden1");
        final String strHidden2 = getArg("hidden2");
        ImageMLDataSet imageDataSet = this.training;
        imageDataSet.downsample(this.downsampleHeight, this.downsampleWidth);
        final int hidden1 = Integer.parseInt(strHidden1);
        final int hidden2 = Integer.parseInt(strHidden2);

        ElmanPattern pattern = new ElmanPattern();
        pattern.setInputNeurons(this.training.getInputSize());
        pattern.setOutputNeurons(this.training.getIdealSize());
        pattern.addHiddenLayer(hidden1);
        pattern.setActivationFunction(new ActivationElliott());

        this.network = (BasicNetwork)pattern.generate();

//        BasicLayer input, hiddenLayer1, hiddenLayer2;
//        this.network = new BasicNetwork();
//        network.addLayer(input = new BasicLayer(new ActivationElliott(), true, this.training.getInputSize()));
//        network.addLayer(hiddenLayer1 = new BasicLayer(new ActivationElliott(), true, hidden1));
//        network.addLayer(hiddenLayer2 = new BasicLayer(new ActivationElliott(), true, hidden2));
//        network.addLayer(new BasicLayer(null, false, this.training.getIdealSize()));
//        input.setContextFedBy(hiddenLayer1);
//        hiddenLayer1.setContextFedBy(hiddenLayer2);
//        network.getStructure().finalizeStructure();
//        network.reset();

//        this.network = EncogUtility.simpleFeedForward(
//                this.training.getInputSize(), hidden1, hidden2,
//                this.training.getIdealSize(), false
//        );
        System.out.println("Created network: " + this.network.toString());
    }
    private void processTrain() throws IOException {
        final String strMode = getArg("mode");
        final String strMinutes = getArg("minutes");
        final String strStrategyError = getArg("strategyerror");
        final String strStrategyCycles = getArg("strategycycles");
        final String strRegularization = getArg("regularization");
        System.out.println("Training Beginning... Output patterns=" + this.outputCount);
        final double strategyError = Double.parseDouble(strStrategyError);
        final int strategyCycles = Integer.parseInt(strStrategyCycles);
        final double regularization = Double.parseDouble(strRegularization);
        final ResilientPropagation train = new ResilientPropagation(this.network, new FoldedDataSet(this.training));
        train.addStrategy(new ResetStrategy(strategyError, strategyCycles));
        //train.addStrategy(new StopTrainingStrategy(0.01, 100));

        if (regularization > 0)
            train.addStrategy(new RegularizationStrategy(regularization));

        final CrossValidationKFold kFoldTrain = new CrossValidationKFold(train, 2);
        if (strMode.equalsIgnoreCase("gui")) {
            TrainingDialog.trainDialog(kFoldTrain, this.network, this.training);
        } else {
            final int minutes = Integer.parseInt(strMinutes);
            int iteration = 0;
            double error = 0.0001;
            long remaining;

            System.out.println("Beginning training...");
            final long start = System.currentTimeMillis();
            do {
                train.iteration();

                final long current = System.currentTimeMillis();
                final long elapsed = (current - start) / 1000; // seconds
                remaining = minutes - elapsed / 60;

                iteration++;

                System.out.println("Iteration #" + Format.formatInteger(iteration)
                                + " Error:" + Format.formatPercent(train.getError())
                                + " elapsed time = " + Format.formatTimeSpan((int) elapsed)
                                + " time left = "
                                + Format.formatTimeSpan((int) remaining * 60));

            } while (remaining > 0 && train.getError() > error && !train.isTrainingDone());
            train.finishTraining();
        }
        System.out.println("Training Stopped...");
    }
    public void processWhatIs() throws IOException {
        final String imageDir = getArg("directory");
        final String type = getArg("type");

        final File directory = new File(imageDir);
        for (String fileName : directory.list(new FilenameFilter() {
                                                  @Override
                                                  public boolean accept(final File dir, final String name)
                                                  {
                                                      return name.matches(".*\\." + type);
                                                  }
                                              })) {
            final Image img = ImageIO.read(new File(directory, fileName));
            final ImageMLData input = new ImageMLData(img);
            input.downsample(this.downsample, false, this.downsampleHeight, this.downsampleWidth, 1, -1);
            final int winner = this.network.winner(input);
            System.out.println("What is: "
                            + fileName + ", it seems to be: "
                            + this.neuron2identity.get(winner));
        }
    }
    private void processSave()
    {
        FileWriter fStream = null;
        BufferedWriter writer = null;
        try
        {
            // write name -> int correspondence i.e. "A" -> 2
            fStream = new FileWriter("identity2neuron.txt");
            writer = new BufferedWriter(fStream);
            for (Map.Entry<String, Integer> entrySet : identity2neuron.entrySet())
            {
                writer.write(entrySet.getKey() + ":" + entrySet.getValue() + "\n");
            }
            writer.close();

            EncogDirectoryPersistence.saveObject(new File("network.txt"), this.network);
            System.out.println("Network persisted to disk (see identity2neuron.txt and network.txt)...");
        }
        catch (Exception ex)
        {
        }
        finally
        {
            try
            {
                if (writer != null)
                    writer.close();
                if (fStream != null)
                    fStream.close();
            } catch (IOException ioex) {}
        }
    }
    private void processRestore()
    {
        File network = new File("network.txt");
        if (!network.exists())
            throw new IllegalArgumentException("network description file doesn't exist!");
        File identity2neuron = new File("identity2neuron.txt");
        if (!identity2neuron.exists())
            throw new IllegalArgumentException("neuron identity file doesn't exist!");

        this.network = (BasicNetwork)EncogDirectoryPersistence.loadObject(network);

        FileReader fStream = null;
        BufferedReader br = null;
        try
        {
            fStream = new FileReader(identity2neuron);
            br = new BufferedReader(fStream);

            String line;
            while ((line = br.readLine()) != null)
            {
                if (!line.isEmpty())
                {
                    String[] tokens = line.split(":");
                    String key = tokens[0];
                    Integer value = Integer.valueOf(tokens[1]);

                    this.identity2neuron.put(key, value);
                    if (!neuron2identity.containsKey(value))
                        this.neuron2identity.put(value, key);
                }
            }
        }
        catch (Exception ex)
        {

        }
        finally
        {
            try
            {
                if (br != null)
                    br.close();
                if (fStream != null)
                    fStream.close();
            } catch (IOException ioex) {}
        }
        System.out.println("Network restored from disk...");
    }
}
