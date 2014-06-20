package sn.demo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

import sn.recover.SensorData;
import sn.regiondetect.ComplexRegion;

public class GeneratorMainEntry {

	public static void main(String args[]) throws Exception {

		Random r = new Random();

		int nCases = 1000;

		int width = 800; // width of canvas
		int height = 600; // height of canvas

		int lineGap = 20; // Gap between lines (uniform)
		int lineSet = 3; // indicates number of sets of parallel lines to be
							// drawn

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			String[] msg = arg.split("=");

			if (msg.length < 2) {
				System.err.println("ERROR: Parameter " + msg[0]
						+ " has no value!");
				System.out.println("Expect:	<Parameter>=<Value>");
				System.exit(-1);
			}

			if (msg[0].equals("width") || msg[0].equals("Width")
					|| msg[0].equals("WIDTH")) {
				try {
					width = Integer.parseInt(msg[1]);
				} catch (Exception e) {
					System.err
							.println("ERROR: Illigal value type for parameter "
									+ msg[0]);
					System.out.println("Expect:	<Integer>");
					System.exit(-1);
				}

			}

			else if (msg[0].equals("height") || msg[0].equals("Height")
					|| msg[0].equals("HEIGHT")) {
				try {
					height = Integer.parseInt(msg[1]);
				} catch (Exception e) {
					System.err
							.println("ERROR: Illigal value type for parameter "
									+ msg[0]);
					System.out.println("Expect:	<Integer>");
					System.exit(-1);
				}
			}

			else if (msg[0].equals("nCases")) {
				try {
					nCases = Integer.parseInt(msg[1]);
				} catch (Exception e) {
					System.err
							.println("ERROR: Illigal value type for parameter "
									+ msg[0]);
					System.out.println("Expect:	<Integer>");
					System.exit(-1);
				}
			}

			else if (msg[0].equals("gap") || msg[0].equals("Gap")
					|| msg[0].equals("GAP")) {
				try {
					lineGap = Integer.parseInt(msg[1]);
				} catch (Exception e) {
					System.err
							.println("ERROR: Illigal value type for parameter "
									+ msg[0]);
					System.out.println("Expect:	<Integer>");
					System.exit(-1);
				}
			}

			else if (msg[0].equals("nSensorSets")) {
				try {
					lineSet = Integer.parseInt(msg[1]);
				} catch (Exception e) {
					System.err
							.println("ERROR: Illigal value type for parameter "
									+ msg[0]);
					System.out.println("Expect:	<Integer>");
					System.exit(-1);
				}
			}

			else {
				System.err.println("ERROR: Illegal paremeter " + msg[0]);
				System.out
						.println("USAGE: java -jar RegionGenerator.jar parameters...");
				System.out.println("parameters:");
				System.out.println("	width=<Integer>");
				System.out.println("	height=<Integer>");
				System.out.println("	nCases=<Integer>");
				System.out.println("	nSensorSets=<Integer>");
				System.out.println("	gap=<Integer>");
				System.exit(-1);
			}

		}
		// Read file name count if exists
		int caseFileCount = 0;
		File file = new File("data/CaseCount.ini");
		if (file.exists()) {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new FileReader(file));
				String nCase = null;
				if ((nCase = reader.readLine()) != null) {
					caseFileCount = Integer.parseInt(nCase);
				}
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			if (file.createNewFile()) {
				System.out.println("created data/CaseCount.ini");
				BufferedWriter output = new BufferedWriter(new FileWriter(file));
				output.write("0");
				output.close();
			} else {
				System.err.println("failed to created data/CaseCount.ini");
			}
		}

		for (int i = 0; i < nCases; i++) {
			caseFileCount++;
			System.out.println("=====================\nGenerating Case "
					+ (i + 1) + "/" + nCases);
			ComplexRegion complexRegion = new ComplexRegion(width, height);
			SensorData d = null;
			String fileHead = String.format("data/test%d", caseFileCount);
			String fileName;
			double[] lineAngle = new double[lineSet];// angle of lines
			for (int n = 0; n < lineSet; n++) {
				if (r.nextBoolean())
					lineAngle[n] = r.nextDouble() * Math.PI / 2.1;
				else
					lineAngle[n] = r.nextDouble() * Math.PI * (1 - 1 / 1.9)
							+ Math.PI / 1.9;

				d = new SensorData(complexRegion, lineGap, lineAngle[n],
						complexRegion.getWidth(), complexRegion.getHeight());
				fileName = String.format(
						fileHead + "-positiveInterval[%d].png", n);
				d.drawPositiveIntervals(fileName,complexRegion.drawRegion(),false);

				String positiveFileName, negativeFileName;
				positiveFileName = String.format(
						fileHead + "-positiveData[%d]", n);
				negativeFileName = String.format(
						fileHead + "-negativeData[%d]", n);
				d.writeIntervalsToFile(positiveFileName, negativeFileName,
						false);

				positiveFileName = String.format(fileHead
						+ "-positiveDataNorm[%d]", n);
				negativeFileName = String.format(fileHead
						+ "-negativeDataNorm[%d]", n);
				d.writeIntervalsToFile(positiveFileName, negativeFileName, true);
			}
			fileName = fileHead;
			complexRegion.saveRegion(i, fileName, true);

		}

		BufferedWriter output = new BufferedWriter(new FileWriter(file));
		output.write(String.valueOf(caseFileCount));
		output.close();

	}
}
