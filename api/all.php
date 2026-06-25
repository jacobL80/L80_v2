<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Methods: GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/config.php';

try {
    $pdo = new PDO(
        'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
        DB_USER, DB_PASS,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database connection failed']);
    exit;
}

function hasFullDate(string $s): bool {
    $p = explode('/', $s);
    if (count($p) === 3) return true;
    if (count($p) === 2 && (int)$p[1] <= 31) return true; // M/D
    return false;
}

// Resolves M/D to M/D/YYYY (next upcoming occurrence) so all returned dates are 3-part.
function resolveDate(string $s): string {
    $p = explode('/', $s);
    if (count($p) === 3) return $s;
    if (count($p) === 2 && (int)$p[1] <= 31) {
        $year      = (int)date('Y');
        $candidate = mktime(0, 0, 0, (int)$p[0], (int)$p[1], $year);
        $today     = mktime(0, 0, 0, (int)date('n'), (int)date('j'), $year);
        if ($candidate < $today) $year++;
        return $p[0] . '/' . $p[1] . '/' . $year;
    }
    return $s;
}

function toSortTs(string $s): int {
    $s = resolveDate($s);
    $p = explode('/', $s);
    if (count($p) !== 3) return PHP_INT_MAX;
    $month = (int)$p[0];
    $day   = (int)$p[1];
    $year  = (int)$p[2];
    if ($year < 100) $year += ($year < 50 ? 2000 : 1900);
    $ts = mktime(0, 0, 0, $month, $day, $year);
    return $ts === false ? PHP_INT_MAX : (int)$ts;
}

$items = [];

// Music — artists with upcoming full dates
try {
    $rows = $pdo->query(
        'SELECT name, next_release, album_title, url FROM artists WHERE next_release != "" ORDER BY id'
    )->fetchAll(PDO::FETCH_ASSOC);
    foreach ($rows as $r) {
        if (hasFullDate($r['next_release'])) {
            $items[] = [
                'type'     => 'music',
                'date'     => resolveDate($r['next_release']),
                'title'    => $r['name'],
                'subtitle' => $r['album_title'],
                'url'      => $r['url'] ?? '',
                '_sort'    => toSortTs($r['next_release']),
            ];
        }
    }
} catch (PDOException $e) {}

// Concerts — not yet attended, full date
try {
    $rows = $pdo->query(
        'SELECT band, tour_name, venue, date FROM concerts WHERE attended = 0 AND date != "" ORDER BY id'
    )->fetchAll(PDO::FETCH_ASSOC);
    foreach ($rows as $r) {
        if (hasFullDate($r['date'])) {
            $sub = array_filter([$r['tour_name'], $r['venue']]);
            $items[] = [
                'type'     => 'concert',
                'date'     => resolveDate($r['date']),
                'title'    => $r['band'],
                'subtitle' => implode(' · ', $sub),
                'url'      => '',
                '_sort'    => toSortTs($r['date']),
            ];
        }
    }
} catch (PDOException $e) {}

// TV/Movies — not yet watched, full date
try {
    $rows = $pdo->query(
        'SELECT program_name, service, date, type FROM tv_shows WHERE watched = 0 AND date != "" ORDER BY id'
    )->fetchAll(PDO::FETCH_ASSOC);
    foreach ($rows as $r) {
        if (hasFullDate($r['date'])) {
            $items[] = [
                'type'     => 'tv',
                'showType' => $r['type'] ?? '',
                'date'     => resolveDate($r['date']),
                'title'    => $r['program_name'],
                'subtitle' => $r['service'],
                'url'      => '',
                '_sort'    => toSortTs($r['date']),
            ];
        }
    }
} catch (PDOException $e) {}

usort($items, fn($a, $b) => $a['_sort'] <=> $b['_sort']);

echo json_encode(array_map(function($item) {
    unset($item['_sort']);
    return $item;
}, $items));
