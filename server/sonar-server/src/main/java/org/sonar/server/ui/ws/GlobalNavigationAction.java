/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.ui.ws;

import org.sonar.api.config.Settings;
import org.sonar.api.server.ws.Request;
import org.sonar.api.server.ws.Response;
import org.sonar.api.server.ws.WebService.NewController;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.NavigationSection;
import org.sonar.api.web.Page;
import org.sonar.core.dashboard.ActiveDashboardDao;
import org.sonar.core.dashboard.DashboardDto;
import org.sonar.server.ui.ViewProxy;
import org.sonar.server.ui.Views;
import org.sonar.server.user.UserSession;

import java.util.List;

public class GlobalNavigationAction implements NavigationAction {

  private final ActiveDashboardDao activeDashboardDao;
  private final Views views;
  private final Settings settings;

  public GlobalNavigationAction(ActiveDashboardDao activeDashboardDao, Views views, Settings settings) {
    this.activeDashboardDao = activeDashboardDao;
    this.views = views;
    this.settings = settings;
  }

  @Override
  public void define(NewController context) {
    context.createAction("global")
      .setDescription("Get information concerning global navigation for the current user.")
      .setHandler(this)
      .setInternal(true)
      .setResponseExample(getClass().getResource("example-global.json"))
      .setSince("5.2");
  }

  @Override
  public void handle(Request request, Response response) throws Exception {
    UserSession userSession = UserSession.get();

    List<DashboardDto> dashboards = activeDashboardDao.selectGlobalDashboardsForUserLogin(userSession.login());

    JsonWriter json = response.newJsonWriter().beginObject();
    writeDashboards(json, dashboards);
    writePages(json);
    writeLogoProperties(json);
    json.endObject().close();
  }

  private void writeDashboards(JsonWriter json, List<DashboardDto> dashboards) {
    json.name("globalDashboards").beginArray();
    for (DashboardDto dashboard : dashboards) {
      json.beginObject()
        .prop("key", dashboard.getKey())
        .prop("name", dashboard.getName())
        .endObject();
    }
    json.endArray();
  }

  private void writePages(JsonWriter json) {
    json.name("globalPages").beginArray();
    for (ViewProxy<Page> page : views.getPages(NavigationSection.HOME)) {
      json.beginObject()
        .prop("name", page.getTitle())
        .prop("url", page.isController() ? page.getId() : String.format("/plugins/home/%s", page.getId()))
        .endObject();
    }
    json.endArray();
  }

  private void writeLogoProperties(JsonWriter json) {
    json.prop("logoUrl", settings.getString("sonar.lf.logoUrl"));
    json.prop("logoWidth", settings.getString("sonar.lf.logoWidthPx"));
  }

}
