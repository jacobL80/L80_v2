<?php
// Upload this file to: public_html/api/history.php

header('Content-Type: application/json');
header('Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-Edit-Token');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/config.php';

try {
    $pdo = new PDO(
        'mysql:host=' . DB_HOST . ';dbname=' . DB_NAME . ';charset=utf8mb4',
        DB_USER,
        DB_PASS,
        [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION]
    );
} catch (PDOException $e) {
    http_response_code(500);
    echo json_encode(['error' => 'Database connection failed']);
    exit;
}

function verify_auth() {
    $token = $_SERVER['HTTP_X_EDIT_TOKEN'] ?? '';
    if ($token !== EDIT_SECRET) {
        http_response_code(401);
        echo json_encode(['error' => 'Unauthorized']);
        exit;
    }
}

function row_to_entry(array $r): array {
    return [
        'id'           => (int) $r['id'],
        'artist_name'  => $r['artist_name'],
        'album_title'  => $r['album_title'],
        'release_date' => $r['release_date'],
        'acquired_at'  => $r['acquired_at'],
    ];
}

$method = $_SERVER['REQUEST_METHOD'];
$id     = isset($_GET['id']) ? (int) $_GET['id'] : null;

if ($method === 'GET') {
    $rows = $pdo->query('SELECT * FROM releases ORDER BY acquired_at DESC, id DESC')
                ->fetchAll(PDO::FETCH_ASSOC);
    echo json_encode(array_map('row_to_entry', $rows));
    exit;
}

if ($method === 'POST') {
    verify_auth();
    $body = json_decode(file_get_contents('php://input'), true);
    $stmt = $pdo->prepare(
        'INSERT INTO releases (artist_name, album_title, release_date, acquired_at)
         VALUES (:an, :at, :rd, CURDATE())'
    );
    $stmt->execute([
        'an' => trim($body['artist_name'] ?? ''),
        'at' => trim($body['album_title']  ?? ''),
        'rd' => trim($body['release_date'] ?? ''),
    ]);
    $newId = (int) $pdo->lastInsertId();
    $row   = $pdo->query("SELECT * FROM releases WHERE id = $newId")->fetch(PDO::FETCH_ASSOC);
    http_response_code(201);
    echo json_encode(row_to_entry($row));
    exit;
}

if ($method === 'DELETE' && $id) {
    verify_auth();
    $pdo->prepare('DELETE FROM releases WHERE id = ?')->execute([$id]);
    echo json_encode(['success' => true]);
    exit;
}

http_response_code(405);
echo json_encode(['error' => 'Method not allowed']);
