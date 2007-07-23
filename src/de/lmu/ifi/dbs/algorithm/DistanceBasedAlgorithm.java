package de.lmu.ifi.dbs.algorithm;

import java.util.List;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Provides an abstract algorithm already setting the distance funciton.
 *
 * @author Arthur Zimek 
 */
public abstract class DistanceBasedAlgorithm<O extends DatabaseObject, D extends Distance<D>>
    extends AbstractAlgorithm<O> {

  /**
   * The default distance function.
   */
  public static final String DEFAULT_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

  /**
   * Parameter for distance function.
   */
  public static final String DISTANCE_FUNCTION_P = "distancefunction";

  /**
   * Description for parameter distance function.
   */
  public static final String DISTANCE_FUNCTION_D = "the distance function to determine the distance between database objects "
                                                   + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class)
                                                   + ". Default: "
                                                   + DEFAULT_DISTANCE_FUNCTION;

  /**
   * The distance function.
   */
  private DistanceFunction<O, D> distanceFunction;

  /**
   * Adds parameter for distance function to parameter map.
   */
  protected DistanceBasedAlgorithm() {
    super();
    // parameter distance function
    ClassParameter<DistanceFunction<O,D>> distance = new ClassParameter(DISTANCE_FUNCTION_P, DISTANCE_FUNCTION_D, DistanceFunction.class);
    distance.setDefaultValue(DEFAULT_DISTANCE_FUNCTION);
    optionHandler.put(DISTANCE_FUNCTION_P, distance);
  }

  /**
   * Calls
   * {@link AbstractAlgorithm#setParameters(String[]) AbstractAlgorithm#setParameters(args)}
   * and sets additionally the distance function, passing remaining parameters
   * to the set distance function.
   *
   * @see AbstractAlgorithm#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    String className = (String)optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
    try {
      // noinspection unchecked
      distanceFunction = Util.instantiate(DistanceFunction.class, className);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(DISTANCE_FUNCTION_P, className, DISTANCE_FUNCTION_D, e);
    }

    remainingParameters = distanceFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  @Override
public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    attributeSettings.addAll(distanceFunction.getAttributeSettings());
    return attributeSettings;
  }

  /**
   * Returns the distanceFunction.
   *
   * @return the distanceFunction
   */
  public DistanceFunction<O, D> getDistanceFunction() {
    return distanceFunction;
  }

}
