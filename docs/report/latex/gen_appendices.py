# Regenerates appendix-classes.tex, appendix-tests.tex and appendix-decisions.tex from the code and docs.
# Usage (from the repository root): python3 docs/report/latex/gen_appendices.py . docs/report/latex
import os, re, sys
ROOT = sys.argv[1]; OUT = sys.argv[2]

def esc(t):
    rep = {'\\': r'\textbackslash{}', '&': r'\&', '%': r'\%', '$': r'\$', '#': r'\#', '_': r'\_',
           '{': r'\{', '}': r'\}', '~': r'\textasciitilde{}', '^': r'\textasciicircum{}'}
    t = ''.join(rep.get(c, c) for c in t)
    for a, b in [('→', r'$\rightarrow$'), ('←', r'$\leftarrow$'), ('≥', r'$\geq$'), ('≤', r'$\leq$'), ('×', r'$\times$'),
                 ('↔', r'$\leftrightarrow$'), ('–', '--'), ('—', '---'), ('…', '...'), ('’', "'"), ('“', '``'), ('”', "''")]:
        t = t.replace(a, b)
    t = t.replace('->', r'$\rightarrow$').replace('<', r'\textless{}').replace('>', r'\textgreater{}')
    return t

def md(t):
    # inline markdown -> latex: `code` and **bold**
    parts = re.split(r'(`[^`]+`)', t)
    out = []
    for p in parts:
        if p.startswith('`') and p.endswith('`'):
            out.append(r'\code{' + esc(p[1:-1]) + '}')
        else:
            q = esc(p)
            q = re.sub(r'\*\*(.+?)\*\*', r'\\textbf{\1}', q)
            q = re.sub(r'\*(.+?)\*', r'\\textit{\1}', q)
            out.append(q)
    return ''.join(out)

# ---------------- class catalogue
base = os.path.join(ROOT, 'src/main/java/com/iitsm')
rows = {}
decl = re.compile(r'^\s*(public\s+|protected\s+)?(abstract\s+|final\s+|sealed\s+)*(class|interface|record|enum)\s+(\w+)', re.M)
for dp, _, fs in os.walk(base):
    for f in sorted(fs):
        if not f.endswith('.java') or f == 'package-info.java': continue
        src = open(os.path.join(dp, f), encoding='utf-8').read()
        m = decl.search(src)
        if not m: continue
        kind, name = m.group(3), m.group(4)
        head = src[:m.start()]
        jd = re.findall(r'/\*\*(.*?)\*/', head, re.S)
        text = ''
        if jd:
            text = re.sub(r'^\s*\*\s?', '', jd[-1], flags=re.M)
            text = re.sub(r'\{@code\s+([^}]+)\}', lambda x: x.group(1), text)
            text = re.sub(r'\{@link(?:plain)?\s+([^}]+)\}', lambda x: x.group(1).split(' ', 1)[1] if ' ' in x.group(1) else x.group(1).split('#')[-1].split('.')[-1], text)
            text = re.sub(r'<pre>.*?</pre>', ' ', text, flags=re.S)
            text = re.sub(r'<[^>]+>', ' ', text)
            text = re.sub(r'@\w+.*', '', text, flags=re.S)
            text = ' '.join(text.split())
            text = text.replace('e.g.', 'e\x00g\x00').replace('i.e.', 'i\x00e\x00')
            sent = [x.replace('\x00', '.') for x in re.split(r'(?<=[.!?])\s', text)]
            text = sent[0]
            if len(text) < 60 and len(sent) > 1: text += ' ' + sent[1]
            text = text.replace('....', '...')
            if len(text) > 260: text = text[:257].rsplit(' ', 1)[0] + '...'
        if 'abstract class' in src[m.start():m.end()+20] or re.search(r'abstract\s+class\s+' + name, src): kind = 'abstract class'
        pkg = os.path.relpath(dp, base).replace(os.sep, '.')
        rows.setdefault(pkg, []).append((name, kind, text))

order = ['domain', 'domain.user', 'domain.incident', 'domain.incident.state', 'domain.catalog', 'domain.sla', 'domain.audit',
         'priority', 'assignment', 'sla', 'event', 'audit', 'notification', 'intelligence', 'service', 'repository',
         'web', 'web.form', 'security', 'exception', 'config', 'bootstrap', '.']
pk = sorted(rows, key=lambda p: (order.index(p) if p in order else 99, p))
with open(os.path.join(OUT, 'appendix-classes.tex'), 'w') as o:
    total = sum(len(v) for v in rows.values())
    o.write('%% generated from the Javadoc of every class in src/main/java (%d types)\n' % total)
    o.write('\\begin{longtable}{@{}L{4.6cm}L{1.9cm}L{8.6cm}@{}}\n\\toprule\n\\textbf{Type} & \\textbf{Kind} & \\textbf{Responsibility} \\\\\n\\midrule\\endhead\n')
    for p in pk:
        label = 'com.iitsm' + ('' if p == '.' else '.' + p)
        o.write('\\multicolumn{3}{@{}l}{\\cellcolor{rowtint}\\textbf{\\code{%s}}} \\\\\n' % esc(label))
        for name, kind, text in sorted(rows[p]):
            o.write('\\code{%s} & %s & %s \\\\\n' % (re.sub(r'(?<=[a-z])(?=[A-Z])', r'\\-', esc(name)), kind, esc(text) if text else '\\textit{(see source)}'))
    o.write('\\bottomrule\n\\end{longtable}\n')
    print('classes', total)

# ---------------- test cases
tc = [l for l in open(os.path.join(ROOT, 'docs/test-cases.md'), encoding='utf-8') if l.startswith('| TC')]
with open(os.path.join(OUT, 'appendix-tests.tex'), 'w') as o:
    o.write('\\begin{longtable}{@{}L{1.0cm}L{1.85cm}L{2.15cm}L{4.05cm}L{5.45cm}@{}}\n\\toprule\n\\textbf{ID} & \\textbf{Area} & \\textbf{Type} & \\textbf{Scenario} & \\textbf{Expected result} \\\\\n\\midrule\\endhead\n')
    for l in tc:
        c = [x.strip() for x in l.strip().strip('|').split('|')]
        o.write(' & '.join(md(x) for x in c[:5]) + ' \\\\\n')
    o.write('\\bottomrule\n\\end{longtable}\n')
    print('tests', len(tc))

# ---------------- decisions
s = open(os.path.join(ROOT, 'docs/decisions.md'), encoding='utf-8').read()
with open(os.path.join(OUT, 'appendix-decisions.tex'), 'w') as o:
    o.write('\\begin{longtable}{@{}L{0.9cm}L{4.3cm}L{10cm}@{}}\n\\toprule\n\\textbf{No.} & \\textbf{Decision} & \\textbf{Alternatives and reasoning} \\\\\n\\midrule\\endhead\n')
    n = 0
    for m in re.finditer(r'## (D\d+)\. (.+?)\n(.*?)(?=\n## D|\Z)', s, re.S):
        body = ' '.join(l.strip().lstrip('-').strip() for l in m.group(3).strip().splitlines() if l.strip())
        if len(body) > 520: body = body[:517].rsplit(' ', 1)[0] + '...'
        o.write('%s & %s & %s \\\\\n' % (m.group(1), md(m.group(2)), md(body))); n += 1
    o.write('\\bottomrule\n\\end{longtable}\n')
    print('decisions', n)
