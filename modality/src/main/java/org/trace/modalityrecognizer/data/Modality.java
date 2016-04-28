package org.trace.modalityrecognizer.data;

/**
 * Supported modalities.
 */
public enum  Modality {
    /** Unknown modality */
    Unknown,
    /** The modality when the user is stationary or still */
    Stationary,
    /** The modality when the user is walking */
    Walking,
    /** The modality when the user is running */
    Running,
    /** The modality when the user is cycling a regular bike */
    Cycling,
    /** The modality when the user is cycling a sports bike */
    SportsCycling,
    /** The modality when the user is cycling an electric bike */
    EBike,
    /** The modality when the user is riding a motorcycle */
    Motorcycle,
    /** The modality when the user is riding a car */
    Car,
    /** The modality when the user is taking the bus */
    Bus,
    /** The modality when the user is taking the train */
    Train,
    /** The modality when the user is taking the tram */
    Tram,
    /** The modality when the user is the subway */
    Subway
}
