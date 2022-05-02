package org.dcsa.core.extendedrequest;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.render.RenderContext;
import org.springframework.data.relational.core.sql.render.SqlRenderer;
import org.springframework.r2dbc.core.PreparedOperation;
import org.springframework.r2dbc.core.binding.BindTarget;
import org.springframework.r2dbc.core.binding.Bindings;

@RequiredArgsConstructor(staticName = "of")
class PreparedQuery implements PreparedOperation<Select> {

  @Getter
  private final Select source;
  private final RenderContext renderContext;
  private final Bindings bindings;

  @Override
  public void bindTo(BindTarget target) {
    bindings.apply(target);
  }

  @Override
  public String toQuery() {
    SqlRenderer sqlRenderer = SqlRenderer.create(this.renderContext);
    return sqlRenderer.render(source);
  }
}
