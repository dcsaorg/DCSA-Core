package org.dcsa.core.model;

import org.dcsa.core.extendedrequest.ExtendedRequest;
import org.springframework.data.relational.core.sql.Join;

import java.lang.annotation.*;

/**
 * Describes join relations that {@link ExtendedRequest} should be aware of
 *
 * <b>Note</b>: In most cases, you want {@link ForeignKey} instead of this
 * annotation.
 *
 * Generally speaking, there are two primary use-cases for this annotation
 * (that are not covered by {@link ForeignKey}).
 *
 * <ul>
 *     <li>Provide filter parameters via JOINs <em>without</em> mapping
 *     another entity on select in a declarative manner.  Note if you need
 *     more control over the join then you want a custom {@link ExtendedRequest}
 *     (and override {@link ExtendedRequest#prepareDBEntityAnalysis()}</li>
 *     <li>Map in an entity (when combined with {@link MapEntity} on the field
 *     containing the entity) where the entity cannot be directly joined with
 *     the original entity (e.g., because you need to join via an "uninteresting"
 *     intermediate entity that you do not want to have mapped or you want
 *     a different nesting than what {@link ForeignKey} would have forced on
 *     the entities).</li>
 * </ul>
 *
 * Example usage (with an "uninteresting" Intermediate entity):
 *
 * <pre>{@code
 *
 * @Data
 * @Table("model")
 * @JoinedWithModel(lhsFieldName = "intermediateId", rhsModel = Intermediate.class, rhsFieldName = "id")
 * @JoinedWithModel(lhsModel = Intermediate.class, lhsFieldName = "innerModelId", rhsModel = InnerModel.class, rhsFieldName = "id")
 * public class Model {
 *
 *     // ... an @Id field (or other fields) could go here
 *
 *     private String intermediateId;
 *
 *     @MapEntity
 *     private InnerModel innerModel;
 * }
 *
 * @Table("intermedate")
 * class Intermediate {
 *     @Id
 *     private String id;
 *
 *     private String innerModelId;
 * }
 *
 * @Table("inner_model")
 * class InnerModel {
 *     @Id
 *     private String id;
 *
 *     // Other fields of interest here.
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ModelJoins.class)
public @interface JoinedWithModel {
    /* Model to join with (right hand side) */
    Class<?> rhsModel();

    /* Model to join from (left hand side).
     * Either this or lhsJoinAlias must be given.
     *
     * The use of Object.class is a sentinel to enable it to be omitted by default.
     */
    Class<?> lhsModel() default Object.class;

    /* If either side is joined more than once, these fields can be used to
     * avoid name clashes.
     */
    String rhsJoinAlias() default "";
    String lhsJoinAlias() default "";

    String rhsFieldName();
    String lhsFieldName();

    Join.JoinType joinType() default Join.JoinType.JOIN;

    String[] filterFields() default {};
}
