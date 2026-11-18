# Generates docs/report/use-case.svg: a UML use-case diagram with lanes per actor.
import sys, html
W, H = 1500, 1120
out = []
add = out.append
add(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}" font-family="Inter, Segoe UI, Arial, sans-serif">')
add('''<defs>
<marker id="open" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="8" markerHeight="8" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10" fill="none" stroke="#475467" stroke-width="1.4"/></marker>
<marker id="tri" viewBox="0 0 12 12" refX="11" refY="6" markerWidth="12" markerHeight="12" orient="auto"><path d="M0,0 L12,6 L0,12 Z" fill="#fff" stroke="#344054" stroke-width="1.2"/></marker>
</defs>''')
add(f'<rect width="{W}" height="{H}" fill="#fff"/>')
SX, SY, SW, SH = 170, 120, 1160, 840     # system boundary
add(f'<rect x="{SX}" y="{SY}" width="{SW}" height="{SH}" rx="10" fill="#f8fafc" stroke="#1f3a5f" stroke-width="2"/>')
add(f'<text x="{SX+70}" y="{SY+30}" text-anchor="middle" font-size="20" font-weight="700" fill="#1f3a5f">I-ITSM</text>')

def actor(x, y, name, color="#1f2933"):
    add(f'<g stroke="{color}" stroke-width="2.2" fill="none"><circle cx="{x}" cy="{y}" r="11"/>'
        f'<line x1="{x}" y1="{y+11}" x2="{x}" y2="{y+42}"/><line x1="{x-18}" y1="{y+22}" x2="{x+18}" y2="{y+22}"/>'
        f'<line x1="{x}" y1="{y+42}" x2="{x-15}" y2="{y+66}"/><line x1="{x}" y1="{y+42}" x2="{x+15}" y2="{y+66}"/></g>')
    for i, part in enumerate(name.split('\n')):
        add(f'<text x="{x}" y="{y+88+i*17}" text-anchor="middle" font-size="15" font-weight="600" fill="{color}">{html.escape(part)}</text>')
    return (x, y + 30)

RX, RY = 98, 21
def uc(x, y, label, fill="#ffffff"):
    add(f'<ellipse cx="{x}" cy="{y}" rx="{RX}" ry="{RY}" fill="{fill}" stroke="#2f5d8a" stroke-width="1.5"/>')
    lines = label.split('\n')
    for i, l in enumerate(lines):
        dy = (i - (len(lines)-1)/2) * 14 + 4.5
        add(f'<text x="{x}" y="{y+dy}" text-anchor="middle" font-size="12.5" fill="#1f2933">{html.escape(l)}</text>')
    return (x, y)

def edge_point(c, toward):
    import math
    (x, y), (tx, ty) = c, toward
    a = math.atan2(ty - y, tx - x)
    return (x + RX*math.cos(a), y + RY*math.sin(a))

def assoc(a, c):
    px, py = edge_point(c, a)
    add(f'<line x1="{a[0]}" y1="{a[1]}" x2="{px:.1f}" y2="{py:.1f}" stroke="#667085" stroke-width="1.1"/>')

def dep(c1, c2, label):
    x1, y1 = edge_point(c1, c2); x2, y2 = edge_point(c2, c1)
    add(f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="#475467" stroke-width="1.3" stroke-dasharray="6 4" marker-end="url(#open)"/>')
    mx, my = (x1+x2)/2, (y1+y2)/2
    add(f'<text x="{mx:.0f}" y="{my-5:.0f}" text-anchor="middle" font-size="11" font-style="italic" fill="#475467" paint-order="stroke" stroke="#f8fafc" stroke-width="4">«{label}»</text>')

def lane(x, title, y=0):
    add(f'<text x="{x}" y="{y}" text-anchor="middle" font-size="12" font-weight="700" fill="#667085" letter-spacing="1">{title}</text>')

# ---- layout
EC, TC, AC = 330, 330, 1180
emp = [uc(EC, 190 + 70*i, t) for i, t in enumerate(["Report incident", "View & track\nown incidents", "Add information /\nanswer request", "Verify resolution &\ngive feedback", "Reopen incident"])]
tec = [uc(TC, 590 + 52*i, t) for i, t in enumerate(["View queue &\nworkload", "Acknowledge\nincident", "Investigate &\nadd notes", "Request information", "Resolve incident", "Set availability", "Confirm / dismiss\nduplicate (also admin)"])]
adm = [uc(AC, 190 + 70*i, t) for i, t in enumerate(["Manage users &\ntechnicians", "Manage IT services\n& categories", "Configure SLA\npolicies", "Choose assignment\nstrategy", "Classify / assign\nincidents", "Monitor dashboard\n& reports", "View audit log\n& outbox"])]
SH_Y = 190
shared = [uc(545, SH_Y, "Register / log in"), uc(760, SH_Y, "Search incidents"), uc(965, SH_Y, "Receive notifications")]
af = "#eef4ff"
calc = uc(640, 330, "Calculate priority", af)
assign = uc(640, 405, "Auto-assign\ntechnician", af)
detect = uc(640, 480, "Detect duplicates", af)
suggest = uc(640, 860, "Suggest category /\nresolution", af)
slamon = uc(960, 720, "Monitor SLA & warn", af)
escal = uc(960, 810, "Escalate unacknowledged\nP1 / P2", af)

E = actor(75, 270, "Employee")
T = actor(75, 690, "Technician")
A = actor(1420, 380, "Administrator")
S = actor(1260, 1000, "System\n(scheduler)")
L = actor(760, 1000, "Team lead")
# abstract User actor (top), with label to the right
add('<g stroke="#475467" stroke-width="2.2" fill="none" stroke-dasharray="0"><circle cx="780" cy="22" r="10"/><line x1="780" y1="32" x2="780" y2="60"/><line x1="764" y1="42" x2="796" y2="42"/><line x1="780" y1="60" x2="767" y2="82"/><line x1="780" y1="60" x2="793" y2="82"/></g>')
add('<text x="802" y="50" font-size="15" font-weight="600" font-style="italic" fill="#475467">User (abstract)</text>')
U = (780, 82)

# generalisation tree: Employee, Technician -> User (left trunk); Administrator -> User (right)
g = 'fill="none" stroke="#344054" stroke-width="1.3"'
add(f'<path d="M 57 300 L 30 300 L 30 720 L 57 720" {g}/>')
add(f'<path d="M 30 300 L 30 100 L 740 100 L 740 70 L 757 70" {g} marker-end="url(#tri)"/>')
add(f'<path d="M 1438 410 L 1475 410 L 1475 100 L 820 100 L 820 70 L 803 70" {g} marker-end="url(#tri)"/>')
# Team lead is a Technician
add(f'<path d="M 742 1030 L 110 1030 L 110 790" {g} marker-end="url(#tri)"/>')

links = []
def link(act, c):
    links.append((act, c)); assoc(act, c)
for c in emp: link(E, c)
for c in tec: link(T, c)
for c in adm: link(A, c)
for c in shared: link(U, c)
link((1260, 1000), slamon); link((1260, 1000), escal); link((760, 1000), escal)

dep(emp[0], calc, "include"); dep(emp[0], assign, "include"); dep(emp[0], detect, "include")
dep(suggest, tec[4], "extend")
ALL = emp + tec + shared + adm + [calc, assign, detect, suggest, slamon, escal]
def hits(p, q, c):
    for k in range(1, 300):
        t = k / 300; x = p[0] + (q[0]-p[0])*t; y = p[1] + (q[1]-p[1])*t
        if ((x-c[0])/RX)**2 + ((y-c[1])/RY)**2 < 0.97: return True
    return False
bad = [(act, c, o) for act, c in links for o in ALL if o != c and hits(act, edge_point(c, act), o)]
print("crossings:", len(bad)); [print(" ", x) for x in bad]
# legend (empty middle area)
lx, ly = 500, 575
add(f'<rect x="{lx-14}" y="{ly-24}" width="430" height="112" rx="8" fill="#ffffff" stroke="#d0d5dd"/>')
for i, line in enumerate(["Legend",
                          "Solid line: the actor takes part in the use case",
                          "Dashed arrow: «include» always happens, «extend» sometimes",
                          "Hollow triangle: generalisation (\u201cis a\u201d)",
                          "Blue use case: done automatically by the system"]):
    w = "700" if i == 0 else "400"
    add(f'<text x="{lx}" y="{ly + i*20}" font-size="13" font-weight="{w}" fill="#344054">{line}</text>')
add('</svg>')
open(sys.argv[1], 'w').write('\n'.join(out))
