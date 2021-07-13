package org.dcsa.core.query.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.dcsa.core.util.ReflectUtility;
import org.springframework.data.relational.core.sql.Join;

import java.util.*;

@RequiredArgsConstructor(staticName = "of")
public class EntityTreeNode {
    @Getter
    private final Class<?> modelType;
    private final String alias;
    @Getter
    @Setter
    private String selectName;
    @Getter
    private final Join.JoinType joinType;
    @Getter
    private final String lhsFieldName;
    @Getter
    private final String rhsFieldName;
    @Getter
    private final List<EntityTreeNode> children = new LinkedList<>();
    private final Map<String, EntityTreeNode> alias2Child = new HashMap<>();
    @Getter
    private final List<QField> queryFields = new LinkedList<>();

    public void addChild(EntityTreeNode child) {
        children.add(child);
        // TODO: Throw exception
        alias2Child.putIfAbsent(child.getAlias(), child);
    }

    public void addQueryField(QField queryField) {
        queryFields.add(queryField);
    }

    public EntityTreeNode getChild(String alias) {
        EntityTreeNode result = alias2Child.get(alias);
        if (result != null) {
            return result;
        }
        for (EntityTreeNode child : children) {
            result = child.getChild(alias);
            if (result != null) {
                return result;
            }
        }
        throw new IllegalArgumentException("No child in entity tree " + this.alias + " with alias: " + alias);
    }

    public String getAlias() {
        if (alias == null || alias.equals("")) {
            return ReflectUtility.getTableName(modelType);
        }
        return alias;
    }
}
