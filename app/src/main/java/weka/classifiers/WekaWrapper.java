package weka.classifiers;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.CapabilitiesHandler;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.RevisionUtils;
import weka.classifiers.Classifier;

import static weka.classifiers.AbstractClassifier.runClassifier;

public class WekaWrapper
        implements Classifier, CapabilitiesHandler {

  /**
   * Returns only the toString() method.
   *
   * @return a string describing the classifier
   */
  public String globalInfo() {
    return toString();
  }

  /**
   * Returns the capabilities of this classifier.
   *
   * @return the capabilities
   */
  public Capabilities getCapabilities() {
    weka.core.Capabilities result = new weka.core.Capabilities(this);

    result.enable(weka.core.Capabilities.Capability.NOMINAL_ATTRIBUTES);
    result.enable(weka.core.Capabilities.Capability.NUMERIC_ATTRIBUTES);
    result.enable(weka.core.Capabilities.Capability.DATE_ATTRIBUTES);
    result.enable(weka.core.Capabilities.Capability.MISSING_VALUES);
    result.enable(weka.core.Capabilities.Capability.NOMINAL_CLASS);
    result.enable(weka.core.Capabilities.Capability.MISSING_CLASS_VALUES);

    result.setMinimumNumberInstances(0);

    return result;
  }

  /**
   * only checks the data against its capabilities.
   *
   * @param i the training data
   */
  public void buildClassifier(Instances i) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(i);
  }

  /**
   * Classifies the given instance.
   *
   * @param i the instance to classify
   * @return the classification result
   */
  public double classifyInstance(Instance i) throws Exception {
    Object[] s = new Object[i.numAttributes()];
    
    for (int j = 0; j < s.length; j++) {
      if (!i.isMissing(j)) {
        if (i.attribute(j).isNominal())
          s[j] = new String(i.stringValue(j));
        else if (i.attribute(j).isNumeric())
          s[j] = new Double(i.value(j));
      }
    }
    
    // set class value to missing
    s[i.classIndex()] = null;
    
    return WekaClassifier.classify(s);
  }

  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {
    return new double[0];
  }

  /**
   * Returns the revision string.
   * 
   * @return        the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("1.0");
  }

  /**
   * Returns only the classnames and what classifier it is based on.
   *
   * @return a short description
   */
  public String toString() {
    return "Auto-generated classifier wrapper, based on weka.classifiers.trees.J48 (generated with Weka 3.6.13).\n" + this.getClass().getName() + "/WekaClassifier";
  }

  /**
   * Runs the classfier from commandline.
   *
   * @param args the commandline arguments
   */
  /*public static void main(String args[]) {
    runClassifier(new WekaWrapper(), args);
  }*/
}

class WekaClassifier {

  public static double classify(Object[] i)
    throws Exception {

    double p = Double.NaN;
    p = WekaClassifier.N366cfbcf0(i);
    return p;
  }
  static double N366cfbcf0(Object []i) {
    double p = Double.NaN;
    if (i[0] == null) {
      p = 0;
    } else if (((Double) i[0]).doubleValue() <= 715.065273) {
    p = WekaClassifier.N2d07eef31(i);
    } else if (((Double) i[0]).doubleValue() > 715.065273) {
    p = WekaClassifier.N6a57c0ef6(i);
    } 
    return p;
  }
  static double N2d07eef31(Object []i) {
    double p = Double.NaN;
    if (i[26] == null) {
      p = 0;
    } else if (((Double) i[26]).doubleValue() <= 1.329396) {
    p = WekaClassifier.N63d8aa9d2(i);
    } else if (((Double) i[26]).doubleValue() > 1.329396) {
    p = WekaClassifier.N56f6e1a35(i);
    } 
    return p;
  }
  static double N63d8aa9d2(Object []i) {
    double p = Double.NaN;
    if (i[0] == null) {
      p = 3;
    } else if (((Double) i[0]).doubleValue() <= 640.365981) {
    p = WekaClassifier.N25a95ca13(i);
    } else if (((Double) i[0]).doubleValue() > 640.365981) {
      p = 0;
    } 
    return p;
  }
  static double N25a95ca13(Object []i) {
    double p = Double.NaN;
    if (i[1] == null) {
      p = 3;
    } else if (((Double) i[1]).doubleValue() <= 0.890328) {
      p = 3;
    } else if (((Double) i[1]).doubleValue() > 0.890328) {
    p = WekaClassifier.N120061584(i);
    } 
    return p;
  }
  static double N120061584(Object []i) {
    double p = Double.NaN;
    if (i[19] == null) {
      p = 0;
    } else if (((Double) i[19]).doubleValue() <= 1.258376) {
      p = 0;
    } else if (((Double) i[19]).doubleValue() > 1.258376) {
      p = 3;
    } 
    return p;
  }
  static double N56f6e1a35(Object []i) {
    double p = Double.NaN;
    if (i[1] == null) {
      p = 3;
    } else if (((Double) i[1]).doubleValue() <= 101.015602) {
      p = 3;
    } else if (((Double) i[1]).doubleValue() > 101.015602) {
      p = 0;
    } 
    return p;
  }
  static double N6a57c0ef6(Object []i) {
    double p = Double.NaN;
    if (i[0] == null) {
      p = 1;
    } else if (((Double) i[0]).doubleValue() <= 1156.204366) {
      p = 1;
    } else if (((Double) i[0]).doubleValue() > 1156.204366) {
      p = 2;
    } 
    return p;
  }
}
