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
    $pdo->exec('CREATE TABLE IF NOT EXISTS tv_shows (
        id           INT AUTO_INCREMENT PRIMARY KEY,
        program_name VARCHAR(255) NOT NULL DEFAULT \'\',
        service      VARCHAR(255) NOT NULL DEFAULT \'\',
        date         VARCHAR(20)  NOT NULL DEFAULT \'\',
        notes        VARCHAR(500) NOT NULL DEFAULT \'\',
        watched      TINYINT(1)   NOT NULL DEFAULT 0
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci');
} catch (PDOException $e) {}

function verify_auth() {
    $token = $_SERVER['HTTP_X_EDIT_TOKEN'] ?? '';
    if ($token !== EDIT_SECRET) {
        http_response_code(401);
        echo json_encode(['error' => 'Unauthorized']);
        exit;
    }
}

function row_to_show(array $row): array {
    return [
        'id'          => (int) $row['id'],
        'programName' => $row['program_name'],
        'service'     => $row['service'],
        'date'        => $row['date'],
        'notes'       => $row['notes'],
        'watched'     => (bool) $row['watched'],
    ];
}

$method = $_SERVER['REQUEST_METHOD'];
$id     = isset($_GET['id']) ? (int) $_GET['id'] : null;

if ($method === 'GET') {
    $rows = $pdo->query('SELECT * FROM tv_shows ORDER BY id')->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode(array_map('row_to_show', $rows));

} elseif ($method === 'POST') {
    verify_auth();
    $body = json_decode(file_get_contents('php://input'), true);
    $stmt = $pdo->prepare(
        'INSERT INTO tv_shows (program_name, service, date, notes, watched)
         VALUES (:prog, :svc, :date, :notes, :watched)'
    );
    $stmt->execute([
        'prog'    => trim($body['programName'] ?? ''),
        'svc'     => trim($body['service'] ?? ''),
        'date'    => trim($body['date'] ?? ''),
        'notes'   => trim($body['notes'] ?? ''),
        'watched' => empty($body['watched']) ? 0 : 1,
    ]);
    $newId = (int) $pdo->lastInsertId();
    $row = $pdo->query("SELECT * FROM tv_shows WHERE id = $newId")->fetch(PDO::FETCH_ASSOC);
    http_response_code(201);
    echo json_encode(row_to_show($row));

} elseif ($method === 'PUT' && $id) {
    verify_auth();
    $body = json_decode(file_get_contents('php://input'), true);
    $stmt = $pdo->prepare(
        'UPDATE tv_shows SET program_name=:prog, service=:svc, date=:date,
         notes=:notes, watched=:watched WHERE id=:id'
    );
    $stmt->execute([
        'prog'    => trim($body['programName'] ?? ''),
        'svc'     => trim($body['service'] ?? ''),
        'date'    => trim($body['date'] ?? ''),
        'notes'   => trim($body['notes'] ?? ''),
        'watched' => empty($body['watched']) ? 0 : 1,
        'id'      => $id,
    ]);
    $row = $pdo->query("SELECT * FROM tv_shows WHERE id = $id")->fetch(PDO::FETCH_ASSOC);
    echo json_encode(row_to_show($row));

} elseif ($method === 'DELETE' && $id) {
    verify_auth();
    $pdo->prepare('DELETE FROM tv_shows WHERE id = ?')->execute([$id]);
    echo json_encode(['success' => true]);

} else {
    http_response_code(405);
    echo json_encode(['error' => 'Method not allowed']);
}
