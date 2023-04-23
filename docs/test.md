## A test

<script src="https://cdn.jsdelivr.net/npm/@gitgraph/js"></script>

<div id="main-version-inc-container"></div>

<!-- Use the `GitgraphJS` global variable to create your graph -->
<script>
var withoutHash = GitgraphJS.templateExtend(GitgraphJS.TemplateName.Metro, {
    commit: {
      message: {
        displayHash: false,
        displayAuthor: false,
        font: "normal 10pt"
      },
      spacing: 40,
      font: "8pt"
    },
    branch: {
      spacing: 40,
      label: {
        font: "normal 10pt"
      }
    }
});

function f1() {
    // Get the graph container HTML element.
    const graphContainer = document.getElementById("main-version-inc-container");

    // Instantiate the graph.
    const gitgraph = GitgraphJS.createGitgraph(graphContainer, {
      template: withoutHash,
    });
    
    // Simulate git commands with Gitgraph API.
    const main = gitgraph.branch("main");
    main
      .commit("some code").tag("v4.2.5")
      .commit("feat: some more code").tag("v4.3.0")
      .commit("fix: a fix")
      .commit("feat: a feature").tag("v4.4.0")
    
    //const develop = gitgraph.branch("develop");
    //develop.commit("Add TypeScript");
    
    //const aFeature = gitgraph.branch("a-feature");
    //aFeature
    //  .commit("feat: Make it work   <-- This will bump the minor version")
    //  .commit("Make it right")
    //  .commit("Make it fast");
    
    //develop.merge(aFeature);
    //develop.commit("Prepare v1");
    
    //main.merge(develop).tag("v1.0.0");
}
</script>
