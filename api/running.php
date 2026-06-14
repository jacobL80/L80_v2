<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-Edit-Token');

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

try {
    $pdo->exec('CREATE TABLE IF NOT EXISTS running_logs (
        id           INT AUTO_INCREMENT PRIMARY KEY,
        run_date     DATE          NOT NULL,
        miles        DECIMAL(5, 2) NOT NULL DEFAULT 0,
        pace_seconds INT           NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');
} catch (PDOException $e) {}

// Migration: add pace_seconds to existing tables
try {
    $pdo->exec('ALTER TABLE running_logs ADD COLUMN pace_seconds INT NULL');
} catch (PDOException $e) {}

function verify_auth() {
    $token = $_SERVER['HTTP_X_EDIT_TOKEN'] ?? '';
    if ($token !== EDIT_SECRET) {
        http_response_code(401);
        echo json_encode(['error' => 'Unauthorized']);
        exit;
    }
}

$method = $_SERVER['REQUEST_METHOD'];
$id     = isset($_GET['id']) ? (int) $_GET['id'] : null;

if ($method === 'GET') {
    $yearFilter = isset($_GET['year']) && $_GET['year'] !== 'all' ? (int) $_GET['year'] : null;

    $rows = $pdo->query('SELECT id, run_date, miles, pace_seconds FROM running_logs ORDER BY run_date ASC')
                ->fetchAll(PDO::FETCH_ASSOC);

    // Aggregate into weeks. Week year = year of its Monday.
    $weeks = [];
    foreach ($rows as $row) {
        $date      = new DateTime($row['run_date']);
        $dow       = (int) $date->format('N'); // 1=Mon … 7=Sun
        $monday    = clone $date;
        if ($dow > 1) $monday->modify('-' . ($dow - 1) . ' days');
        $weekKey   = $monday->format('Y-m-d');
        $weekYear  = (int) $monday->format('Y');
        $dayNames  = ['', 'mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun'];
        $dayName   = $dayNames[$dow];

        if (!isset($weeks[$weekKey])) {
            $weeks[$weekKey] = [
                'weekStart' => $weekKey,
                'year'      => $weekYear,
                'total'     => 0.0,
                'mon' => 0.0, 'tue' => 0.0, 'wed' => 0.0, 'thu' => 0.0,
                'fri' => 0.0, 'sat' => 0.0, 'sun' => 0.0,
                'entries'   => [],
            ];
        }
        $miles = (float) $row['miles'];
        $weeks[$weekKey]['total']     += $miles;
        $weeks[$weekKey][$dayName]    += $miles;
        $weeks[$weekKey]['entries'][]  = [
            'id'          => (int) $row['id'],
            'date'        => $row['run_date'],
            'miles'       => $miles,
            'paceSeconds' => isset($row['pace_seconds']) && $row['pace_seconds'] !== null ? (int) $row['pace_seconds'] : null,
        ];
    }

    $result = array_values($weeks);

    if ($yearFilter) {
        $result = array_values(array_filter($result, fn($w) => $w['year'] === $yearFilter));
    }

    // Newest week first
    usort($result, fn($a, $b) => strcmp($b['weekStart'], $a['weekStart']));

    echo json_encode($result);

} elseif ($method === 'POST') {
    verify_auth();
    $body  = json_decode(file_get_contents('php://input'), true);
    $date  = trim($body['date'] ?? date('Y-m-d'));
    $miles = round((float) ($body['miles'] ?? 0), 2);

    // Parse optional pace in MM:SS format → seconds
    $pace = null;
    if (!empty($body['pace'])) {
        $parts = explode(':', trim($body['pace']));
        if (count($parts) === 2) {
            $paceVal = (int)$parts[0] * 60 + (int)$parts[1];
            if ($paceVal > 0) $pace = $paceVal;
        }
    }

    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date)) {
        http_response_code(400);
        echo json_encode(['error' => 'Invalid date — use YYYY-MM-DD']);
        exit;
    }

    $stmt = $pdo->prepare('INSERT INTO running_logs (run_date, miles, pace_seconds) VALUES (:date, :miles, :pace)');
    $stmt->execute(['date' => $date, 'miles' => $miles, 'pace' => $pace]);
    $newId = (int) $pdo->lastInsertId();
    $row   = $pdo->query("SELECT * FROM running_logs WHERE id = $newId")->fetch(PDO::FETCH_ASSOC);
    http_response_code(201);
    echo json_encode([
        'id'          => (int)$row['id'],
        'date'        => $row['run_date'],
        'miles'       => (float)$row['miles'],
        'paceSeconds' => $row['pace_seconds'] !== null ? (int)$row['pace_seconds'] : null,
    ]);

} elseif ($method === 'PUT' && $id) {
    verify_auth();
    $body  = json_decode(file_get_contents('php://input'), true);
    $date  = trim($body['date'] ?? '');
    $miles = round((float) ($body['miles'] ?? 0), 2);

    $pace = null;
    if (isset($body['pace']) && $body['pace'] !== '' && $body['pace'] !== null) {
        $parts = explode(':', trim((string)$body['pace']));
        if (count($parts) === 2) {
            $paceVal = (int)$parts[0] * 60 + (int)$parts[1];
            if ($paceVal > 0) $pace = $paceVal;
        }
    }

    if (!preg_match('/^\d{4}-\d{2}-\d{2}$/', $date)) {
        http_response_code(400);
        echo json_encode(['error' => 'Invalid date — use YYYY-MM-DD']);
        exit;
    }
    if ($miles <= 0) {
        http_response_code(400);
        echo json_encode(['error' => 'Miles must be greater than 0']);
        exit;
    }

    $stmt = $pdo->prepare('UPDATE running_logs SET run_date = :date, miles = :miles, pace_seconds = :pace WHERE id = :id');
    $stmt->execute(['date' => $date, 'miles' => $miles, 'pace' => $pace, 'id' => $id]);
    $row = $pdo->query("SELECT * FROM running_logs WHERE id = $id")->fetch(PDO::FETCH_ASSOC);
    echo json_encode([
        'id'          => (int)$row['id'],
        'date'        => $row['run_date'],
        'miles'       => (float)$row['miles'],
        'paceSeconds' => $row['pace_seconds'] !== null ? (int)$row['pace_seconds'] : null,
    ]);

} elseif ($method === 'DELETE' && $id) {
    verify_auth();
    $pdo->prepare('DELETE FROM running_logs WHERE id = ?')->execute([$id]);
    echo json_encode(['success' => true]);

} else {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
}
