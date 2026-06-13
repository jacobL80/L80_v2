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
    $pdo->exec('CREATE TABLE IF NOT EXISTS concerts (
        id        INT AUTO_INCREMENT PRIMARY KEY,
        band      VARCHAR(255) NOT NULL DEFAULT \'\',
        tour_name VARCHAR(255) NOT NULL DEFAULT \'\',
        venue     VARCHAR(255) NOT NULL DEFAULT \'\',
        date      VARCHAR(20)  NOT NULL DEFAULT \'\',
        notes     VARCHAR(500) NOT NULL DEFAULT \'\',
        attended  TINYINT(1)   NOT NULL DEFAULT 0,
        attendees VARCHAR(500) NOT NULL DEFAULT \'\'
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');
} catch (PDOException $e) {}

// Add attendees column to existing tables that predate this field
try {
    $pdo->exec("ALTER TABLE concerts ADD COLUMN attendees VARCHAR(500) NOT NULL DEFAULT ''");
} catch (PDOException $e) {}

function verify_auth() {
    $token = $_SERVER['HTTP_X_EDIT_TOKEN'] ?? '';
    if ($token !== EDIT_SECRET) {
        http_response_code(401);
        echo json_encode(['error' => 'Unauthorized']);
        exit;
    }
}

function row_to_concert(array $row): array {
    return [
        'id'        => (int) $row['id'],
        'band'      => $row['band'],
        'tourName'  => $row['tour_name'],
        'venue'     => $row['venue'],
        'date'      => $row['date'],
        'notes'     => $row['notes'],
        'attended'  => (bool) $row['attended'],
        'attendees' => $row['attendees'],
    ];
}

$method = $_SERVER['REQUEST_METHOD'];
$id     = isset($_GET['id']) ? (int) $_GET['id'] : null;

if ($method === 'GET') {
    $rows = $pdo->query('SELECT * FROM concerts ORDER BY id')->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode(array_map('row_to_concert', $rows));

} elseif ($method === 'POST') {
    verify_auth();
    $body = json_decode(file_get_contents('php://input'), true);
    $stmt = $pdo->prepare(
        'INSERT INTO concerts (band, tour_name, venue, date, notes, attended, attendees)
         VALUES (:band, :tour, :venue, :date, :notes, :attended, :attendees)'
    );
    $stmt->execute([
        'band'      => trim($body['band'] ?? ''),
        'tour'      => trim($body['tourName'] ?? ''),
        'venue'     => trim($body['venue'] ?? ''),
        'date'      => trim($body['date'] ?? ''),
        'notes'     => trim($body['notes'] ?? ''),
        'attended'  => empty($body['attended']) ? 0 : 1,
        'attendees' => trim($body['attendees'] ?? ''),
    ]);
    $newId = (int) $pdo->lastInsertId();
    $row = $pdo->query("SELECT * FROM concerts WHERE id = $newId")->fetch(PDO::FETCH_ASSOC);
    http_response_code(201);
    echo json_encode(row_to_concert($row));

} elseif ($method === 'PUT' && $id) {
    verify_auth();
    $body = json_decode(file_get_contents('php://input'), true);
    $stmt = $pdo->prepare(
        'UPDATE concerts SET band=:band, tour_name=:tour, venue=:venue, date=:date,
         notes=:notes, attended=:attended, attendees=:attendees WHERE id=:id'
    );
    $stmt->execute([
        'band'      => trim($body['band'] ?? ''),
        'tour'      => trim($body['tourName'] ?? ''),
        'venue'     => trim($body['venue'] ?? ''),
        'date'      => trim($body['date'] ?? ''),
        'notes'     => trim($body['notes'] ?? ''),
        'attended'  => empty($body['attended']) ? 0 : 1,
        'attendees' => trim($body['attendees'] ?? ''),
        'id'        => $id,
    ]);
    $row = $pdo->query("SELECT * FROM concerts WHERE id = $id")->fetch(PDO::FETCH_ASSOC);
    echo json_encode(row_to_concert($row));

} elseif ($method === 'DELETE' && $id) {
    verify_auth();
    $pdo->prepare('DELETE FROM concerts WHERE id = ?')->execute([$id]);
    echo json_encode(['success' => true]);

} else {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
}
