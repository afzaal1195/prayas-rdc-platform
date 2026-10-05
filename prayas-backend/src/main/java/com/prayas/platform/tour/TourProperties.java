package com.prayas.platform.tour;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml:
 *   prayas:
 *     tour:
 *       max-per-day: 3
 *       headcount-freeze-days: 2
 *       requirement-lead-days: 5
 *       min-advance-days: 3
 *       earliest-arrival: "09:30"
 *       latest-departure: "18:00"
 *       min-visit-duration-hours: 3
 */
@ConfigurationProperties(prefix = "prayas.tour")
public class TourProperties {

    /** Max approved tours allowed on a single visit date. Team-set value: 3. */
    private int maxPerDay = 3;

    /** Days before the visit date after which the school can no longer self-edit headcount. Team-set value: 1. */
    private int headcountFreezeDays = 2;

    /** Requirement due_date = visitDate - requirementLeadDays. */
    private int requirementLeadDays = 5;

    /** A visit request must be submitted at least this many days before the visit date. */
    private int minAdvanceDays = 3;

    /** Earliest allowed arrival time on campus. */
    private String earliestArrival = "09:30";

    /** Latest allowed departure time from campus. */
    private String latestDeparture = "18:00";

    /** Minimum required gap between arrival and departure. */
    private int minVisitDurationHours = 3;

    public int getMaxPerDay() {
        return maxPerDay;
    }

    public void setMaxPerDay(int maxPerDay) {
        this.maxPerDay = maxPerDay;
    }

    public int getHeadcountFreezeDays() {
        return headcountFreezeDays;
    }

    public void setHeadcountFreezeDays(int headcountFreezeDays) {
        this.headcountFreezeDays = headcountFreezeDays;
    }

    public int getRequirementLeadDays() {
        return requirementLeadDays;
    }

    public void setRequirementLeadDays(int requirementLeadDays) {
        this.requirementLeadDays = requirementLeadDays;
    }

    public int getMinAdvanceDays() {
        return minAdvanceDays;
    }

    public void setMinAdvanceDays(int minAdvanceDays) {
        this.minAdvanceDays = minAdvanceDays;
    }

    public String getEarliestArrival() {
        return earliestArrival;
    }

    public void setEarliestArrival(String earliestArrival) {
        this.earliestArrival = earliestArrival;
    }

    public String getLatestDeparture() {
        return latestDeparture;
    }

    public void setLatestDeparture(String latestDeparture) {
        this.latestDeparture = latestDeparture;
    }

    public int getMinVisitDurationHours() {
        return minVisitDurationHours;
    }

    public void setMinVisitDurationHours(int minVisitDurationHours) {
        this.minVisitDurationHours = minVisitDurationHours;
    }
}