/*
 * Builds docs/report/report.pdf from report.md + diagrams.md, with every Mermaid diagram rendered.
 *
 * One-time setup (any folder outside the repo is fine):
 *     npm install marked@12.0.2 mermaid@10.9.1 playwright@1.47.2
 *     npx playwright install chromium
 * Run from the repository root, pointing NODE_PATH at that folder's node_modules:
 *     NODE_PATH=/path/to/node_modules node docs/report/build-pdf.cjs
 */
const fs = require('fs');
const path = require('path');
const { marked } = require('marked');
const { chromium } = require('playwright');

const dir = __dirname;
const read = (f) => fs.readFileSync(path.join(dir, f), 'utf8');
const mermaidJs = require.resolve('mermaid/dist/mermaid.min.js');

// Links between the two Markdown files become links inside the one PDF.
function localLinks(md) {
    return md
        .replace(/\]\(diagrams\.md\)/g, '](#diagrams)')
        .replace(/\]\(\.\.\/test-cases\.md\)/g, '](https://github.com/VijaySrivathsan/iitsm/blob/main/docs/test-cases.md)')
        .replace(/\]\(use-case\.puml\)/g, '](https://github.com/VijaySrivathsan/iitsm/blob/main/docs/report/use-case.puml)')
        .replace(/\]\(use-case-svg\.py\)/g, '](https://github.com/VijaySrivathsan/iitsm/blob/main/docs/report/use-case-svg.py)');
}

const renderer = new marked.Renderer();
const baseCode = renderer.code.bind(renderer);
renderer.code = (code, lang, escaped) => lang === 'mermaid'
    ? `<div class="diagram"><pre class="mermaid">${code.replace(/</g, '&lt;')}</pre></div>`
    : baseCode(code, lang, escaped);
marked.use({ renderer });

const report = marked.parse(localLinks(read('report.md')));
const diagrams = marked.parse(localLinks(read('diagrams.md')).replace(/^# I-ITSM diagrams/m, '# Appendix: diagrams'));

const html = `<!doctype html><html lang="en"><head><meta charset="utf-8"><title>I-ITSM report</title><base href="file://${dir}/">
<style>
  @page { size: A4; margin: 18mm 16mm 18mm 16mm; }
  body { font-family: "Inter", "Segoe UI", system-ui, sans-serif; font-size: 10.5pt; line-height: 1.5; color: #1f2933; }
  h1 { font-size: 20pt; color: #1f3a5f; border-bottom: 2px solid #1f3a5f; padding-bottom: 4pt; }
  h2 { font-size: 14pt; color: #1f3a5f; margin-top: 18pt; break-after: avoid; }
  h3 { font-size: 11.5pt; break-after: avoid; }
  table { border-collapse: collapse; width: 100%; margin: 8pt 0; font-size: 9pt; break-inside: auto; }
  th, td { border: 1px solid #d0d5dd; padding: 3pt 5pt; text-align: left; vertical-align: top; }
  th { background: #eef2f6; }
  tr { break-inside: avoid; }
  code { font-family: ui-monospace, Menlo, Consolas, monospace; font-size: 8.5pt; background: #f2f4f7; padding: 0 2pt; border-radius: 2pt; }
  pre:not(.mermaid) { background: #f8fafc; border: 1px solid #e4e7ec; padding: 6pt; white-space: pre-wrap; font-size: 8.5pt; }
  blockquote { border-left: 3px solid #f79009; margin: 0; padding: 2pt 10pt; background: #fffaeb; }
  hr { border: 0; border-top: 1px solid #e4e7ec; margin: 14pt 0; }
  a { color: #175cd3; text-decoration: none; }
  @page wide { size: A4 landscape; margin: 12mm; }
  @page tall { size: A4 portrait; }
  html, body { page: wide; }
  #report { page: tall; }
  #diagrams { page: wide; break-before: page; }
  #diagrams img { width: 100%; max-height: 135mm; object-fit: contain; break-inside: avoid; }
  #diagrams h2, #diagrams h3 { break-after: avoid; }
  #diagrams ul, #diagrams p, #diagrams table { break-before: avoid; }
  .diagram { break-inside: avoid; text-align: center; margin: 6pt 0; }
  .diagram svg { width: 100% !important; max-width: 100% !important; height: auto !important; max-height: 128mm; }
  pre.mermaid { background: none; border: 0; padding: 0; margin: 0; }
</style></head><body>
<section id="report">${report}</section>
<section id="diagrams">${diagrams}
<script src="file://${mermaidJs}"></script>
<script>
  mermaid.initialize({ startOnLoad: false, theme: 'neutral', securityLevel: 'loose',
                       flowchart: { useMaxWidth: true }, er: { useMaxWidth: true }, class: { useMaxWidth: true } });
  mermaid.run({ querySelector: 'pre.mermaid' })
      .then(() => { document.body.dataset.ready = 'yes'; })
      .catch((e) => { document.body.dataset.ready = 'error: ' + e.message; });
</script></section>
</body></html>`;

(async () => {
    const tmp = path.join(require('os').tmpdir(), 'itsm-report.html');
    fs.writeFileSync(tmp, html);
    const browser = await chromium.launch();
    const page = await browser.newPage();
    await page.goto('file://' + tmp);
    await page.waitForFunction(() => document.body.dataset.ready, null, { timeout: 60000 });
    const state = await page.evaluate(() => document.body.dataset.ready);
    if (state !== 'yes') throw new Error('Mermaid failed: ' + state);
    const failed = await page.$$eval('pre.mermaid', (els) => els.filter((e) => !e.querySelector('svg')).length);
    if (failed) throw new Error(failed + ' diagram(s) did not render');
    const out = path.join(dir, 'report.pdf');
    await page.pdf({
        path: out, format: 'A4', printBackground: true, displayHeaderFooter: true,
        headerTemplate: '<span></span>',
        footerTemplate: '<div style="font-size:8px;width:100%;text-align:center;color:#667085">I-ITSM project report · page <span class="pageNumber"></span> of <span class="totalPages"></span></div>',
        margin: { top: '16mm', bottom: '18mm', left: '15mm', right: '15mm' },
    });
    await browser.close();
    console.log('Wrote ' + out);
})().catch((e) => { console.error(e); process.exit(1); });
