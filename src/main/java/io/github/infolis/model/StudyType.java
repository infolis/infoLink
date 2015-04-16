package io.github.infolis.model;

/**
 * Types of InfoLink links. 
 * <ul>
 * <li>DOI: study names that could be matched to a record in the repository, identified by a DOI</li>
 * <li>URL: study names that are in fact a URL</li>
 * <li>STRING: study names that could not be matched to any record and that are not a URL</li>
 * </ul>
 * 
 * @author katarina.boland@gesis.org
 * @version 2014-01-27
 *
 */
public enum StudyType { DOI, URL, STRING; }