package sonatypestats

import java.nio.file.*

def renderStats(data: CompleteData): (Path, Path) = {
  val json = encodeJson(data)
  val jsonFile = Path.of("data/data.json")
  Files.writeString(jsonFile, json)

  val html = s"""<!DOCTYPE html>
  <head>
   <title>Sonatype stats</title>
   <script>
    const data = $json;
   </script>
   <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
   <script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-autocolors"></script>
  </head>
  <body>
    <canvas id="stats-chart" style="width: 100%; height: 66vh;"></canvas>
    <div id="legend-container" style="width: 100%; height: 34vh; overflow: scroll;"></div>
    <script>
      // list of all organizations
      const organizations = Object.keys(data.data);

      // list of all months
      const months = [...new Set(Object.values(data.data).flatMap(Object.keys))].sort();

      const organizationsDatasets = organizations.map(organization => {
        const timePoints = Object.values(data.data[organization]);
        return ({
          "label": `$${organization} (unique IPs)`,
          "data": timePoints.map(timePoint => timePoint.timeline.total).reverse()
        });
      });

      const artifactDatasets = organizations.flatMap(organization => {
        const timePoints = Object.values(data.data[organization]);
        const artifactNames = [...new Set(timePoints.flatMap(timePoint => 
          (timePoint && timePoint.downloads) ? Object.keys(timePoint.downloads) : []
        ))].sort();
        return artifactNames.flatMap(artifact => {
          return [
            {
              "label": `$${organization}::$${artifact} (unique IPs)`,
              "data": timePoints.map(timePoint => (timePoint && timePoint.uniqueIps) ? timePoint.uniqueIps[artifact] : 0).reverse(),
              "hidden": true
            },
            {
              "label": `$${organization}::$${artifact} (downloads)`,
              "data": timePoints.map(timePoint => (timePoint && timePoint.downloads) ? timePoint.downloads[artifact] : 0).reverse(),
              "hidden": true
            }
          ];
        });
      });

      const getOrCreateLegendList = (chart, id) => {
        const legendContainer = document.getElementById(id);
        let listContainer = legendContainer.querySelector('ul');

        if (!listContainer) {
          listContainer = document.createElement('ul');
          listContainer.style.display = 'block';
          listContainer.style.flexDirection = 'row';
          listContainer.style.margin = 0;
          listContainer.style.padding = 0;

          legendContainer.appendChild(listContainer);
        }

        return listContainer;
      };

      const autocolors = window['chartjs-plugin-autocolors'];

      const htmlLegendPlugin = {
        id: 'htmlLegend',
        afterUpdate(chart, args, options) {
          const ul = getOrCreateLegendList(chart, options.containerID);

          // Remove old legend items
          while (ul.firstChild) {
            ul.firstChild.remove();
          }

          // Reuse the built-in legendItems generator
          const items = chart.options.plugins.legend.labels.generateLabels(chart);

          items.forEach(item => {
            const li = document.createElement('li');
            li.style.alignItems = 'center';
            li.style.cursor = 'pointer';
            li.style.display = 'flex';
            li.style.flexDirection = 'row';
            li.style.marginLeft = '10px';

            li.onclick = () => {
              const {type} = chart.config;
              if (type === 'pie' || type === 'doughnut') {
                // Pie and doughnut charts only have a single dataset and visibility is per item
                chart.toggleDataVisibility(item.index);
              } else {
                chart.setDatasetVisibility(item.datasetIndex, !chart.isDatasetVisible(item.datasetIndex));
              }
              chart.update();
            };

            // Color box
            const boxSpan = document.createElement('span');
            boxSpan.style.background = item.fillStyle;
            boxSpan.style.borderColor = item.strokeStyle;
            boxSpan.style.borderWidth = item.lineWidth + 'px';
            boxSpan.style.display = 'inline-block';
            boxSpan.style.flexShrink = 0;
            boxSpan.style.height = '20px';
            boxSpan.style.marginRight = '10px';
            boxSpan.style.width = '20px';

            // Text
            const textContainer = document.createElement('p');
            textContainer.style.color = item.fontColor;
            textContainer.style.margin = 0;
            textContainer.style.padding = 0;
            textContainer.style.textDecoration = item.hidden ? 'line-through' : '';

            const text = document.createTextNode(item.text);
            textContainer.appendChild(text);

            li.appendChild(boxSpan);
            li.appendChild(textContainer);
            ul.appendChild(li);
          });
        }
      };

      const ctx = document.getElementById('stats-chart');
      const cfg = {
        type: 'line',
        data: {
          labels: months,
          datasets: [...organizationsDatasets, ...artifactDatasets]
        },
        options: {
          plugins: {
            autocolors: {
              offset: organizations.length,
              repeat: 1
            },
            htmlLegend: {
              containerID: 'legend-container',
            },
            legend: {
              display: false,
            }
          },
          responsive: true
        },
        plugins: [autocolors, htmlLegendPlugin],
      };

      new Chart(ctx, cfg);
    </script>
  </body>
</html>
"""
  val htmlFile = Path.of("data/index.html")
  Files.writeString(htmlFile, html)
  (jsonFile, htmlFile)
}
