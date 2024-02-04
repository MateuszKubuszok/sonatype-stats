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
  </head>
  <body>
    <canvas id="stats-chart" />
    <script>
      // list of all organizations
      const organizations = Object.keys(data.data);

      const labels = [...new Set(Object.values(data.data).flatMap(Object.keys))].sort();

      const organizationsDatasets = organizations.map(name => 
        ({
          "label": name,
          "data": Object.values(data.data[name]).map(v => v.timeline.total).reverse()
        })
      );

      const ctx = document.getElementById('stats-chart');
      const cfg = {
        type: 'line',
        data: {
          labels: labels,
          datasets: organizationsDatasets
        }
      };

      new Chart(ctx, cfg);
    </script>
  </body>
</html>
"""
  val htmlFile = Path.of("data/index.html")
  Files.writeString(jsonFile, json)
  (jsonFile, htmlFile)
}
